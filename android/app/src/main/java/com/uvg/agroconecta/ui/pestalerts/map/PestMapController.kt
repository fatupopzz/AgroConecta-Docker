package com.uvg.agroconecta.ui.pestalerts.map

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.max

sealed interface PestMapLoadState {
    data object Loading : PestMapLoadState
    data object Ready : PestMapLoadState
    data class Error(val message: String) : PestMapLoadState
}

internal interface PestMapHost {
    val view: View

    fun attach(lifecycle: Lifecycle)
    fun saveState(outState: Bundle)
    fun retryStyle()
    fun updateUserLocation(location: PestMapCoordinate?, centerCamera: Boolean)
    fun centerOnUser()
    fun dispose()
}

internal fun interface PestMapHostFactory {
    fun create(
        context: Context,
        savedState: Bundle?,
        cameraState: PestMapCameraState,
        onLoadStateChanged: (PestMapLoadState) -> Unit,
        onCameraChanged: (PestMapCameraState) -> Unit,
        onTrackingModeChanged: (PestMapTrackingMode) -> Unit
    ): PestMapHost
}

internal val LocalPestMapHostFactory = staticCompositionLocalOf {
    PestMapHostFactory { context, savedState, cameraState, onLoadStateChanged, onCameraChanged, onTrackingModeChanged ->
        PestMapController(
            context = context,
            savedState = savedState,
            cameraState = cameraState,
            onLoadStateChanged = onLoadStateChanged,
            onCameraChanged = onCameraChanged,
            onTrackingModeChanged = onTrackingModeChanged
        )
    }
}

internal class PestMapController(
    context: Context,
    savedState: Bundle?,
    cameraState: PestMapCameraState,
    private val onLoadStateChanged: (PestMapLoadState) -> Unit,
    private val onCameraChanged: (PestMapCameraState) -> Unit,
    private val onTrackingModeChanged: (PestMapTrackingMode) -> Unit
) : PestMapHost, DefaultLifecycleObserver {

    private val mapView: MapView
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var lifecycle: Lifecycle? = null
    private var userLocation: PestMapCoordinate? = null
    private var shouldCenterOnUser = false
    private var isStarted = false
    private var isResumed = false
    private var isDisposed = false
    private val mapViewState = savedState ?: Bundle()

    private val cameraIdleListener = MapLibreMap.OnCameraIdleListener {
        map?.cameraPosition?.let { position ->
            onCameraChanged(PestMapCameraState.from(position))
        }
    }

    private val cameraMoveStartedListener = MapLibreMap.OnCameraMoveStartedListener { reason ->
        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
            shouldCenterOnUser = false
            onTrackingModeChanged(PestMapTrackingMode.FREE_EXPLORATION)
        }
    }

    private val mapLoadFailureListener = MapView.OnDidFailLoadingMapListener {
        if (!isDisposed) {
            onLoadStateChanged(
                PestMapLoadState.Error(
                    "No se pudo cargar el mapa. Verifica tu conexión e inténtalo de nuevo."
                )
            )
        }
    }

    override val view: View
        get() = mapView

    init {
        MapLibre.getInstance(context.applicationContext)
        val options = MapLibreMapOptions.createFromAttributes(context).apply {
            camera(cameraState.toCameraPosition())
            minZoomPreference(PestMapConfig.MIN_ZOOM)
            maxZoomPreference(PestMapConfig.MAX_ZOOM)
            scrollGesturesEnabled(true)
            zoomGesturesEnabled(true)
            rotateGesturesEnabled(true)
            tiltGesturesEnabled(true)
        }
        mapView = MapView(context, options).also { view ->
            view.addOnDidFailLoadingMapListener(mapLoadFailureListener)
            view.onCreate(mapViewState)
            view.getMapAsync { readyMap ->
                if (isDisposed) return@getMapAsync
                map = readyMap
                readyMap.addOnCameraIdleListener(cameraIdleListener)
                readyMap.addOnCameraMoveStartedListener(cameraMoveStartedListener)
                loadStyle()
            }
        }
    }

    override fun attach(lifecycle: Lifecycle) {
        if (isDisposed || this.lifecycle === lifecycle) return
        this.lifecycle?.removeObserver(this)
        this.lifecycle = lifecycle
        lifecycle.addObserver(this)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startMap()
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            resumeMap()
        }
    }

    override fun onStart(owner: LifecycleOwner) = startMap()

    override fun onResume(owner: LifecycleOwner) = resumeMap()

    override fun onPause(owner: LifecycleOwner) = pauseMap()

    override fun onStop(owner: LifecycleOwner) {
        saveState(mapViewState)
        stopMap()
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()

    override fun saveState(outState: Bundle) {
        if (!isDisposed) {
            mapView.onSaveInstanceState(outState)
        }
    }

    override fun retryStyle() {
        if (!isDisposed) {
            loadStyle()
        }
    }

    override fun updateUserLocation(location: PestMapCoordinate?, centerCamera: Boolean) {
        userLocation = location?.takeIf(PestMapCoordinate::isValid)
        shouldCenterOnUser = centerCamera && userLocation != null
        renderUserLocation()
        if (shouldCenterOnUser) {
            centerOnUser()
        }
    }

    override fun centerOnUser() {
        val currentMap = map ?: run {
            shouldCenterOnUser = userLocation != null
            return
        }
        val location = userLocation ?: return
        val currentPosition = currentMap.cameraPosition
        val targetPosition = CameraPosition.Builder(currentPosition)
            .target(LatLng(location.latitude, location.longitude))
            .zoom(max(currentPosition.zoom, PestMapConfig.INITIAL_ZOOM))
            .build()
        shouldCenterOnUser = false
        currentMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(targetPosition),
            CAMERA_ANIMATION_DURATION_MS
        )
    }

    override fun dispose() {
        if (isDisposed) return
        lifecycle?.removeObserver(this)
        lifecycle = null
        saveState(mapViewState)
        pauseMap()
        stopMap()
        map?.removeOnCameraIdleListener(cameraIdleListener)
        map?.removeOnCameraMoveStartedListener(cameraMoveStartedListener)
        map = null
        style = null
        mapView.removeOnDidFailLoadingMapListener(mapLoadFailureListener)
        mapView.onDestroy()
        isDisposed = true
    }

    private fun loadStyle() {
        val readyMap = map ?: return
        onLoadStateChanged(PestMapLoadState.Loading)
        readyMap.setStyle(PestMapConfig.styleUrl) { loadedStyle ->
            if (!isDisposed) {
                style = loadedStyle
                installUserLocationLayers(loadedStyle)
                renderUserLocation()
                if (shouldCenterOnUser) {
                    centerOnUser()
                }
                onLoadStateChanged(PestMapLoadState.Ready)
            }
        }
    }

    private fun installUserLocationLayers(style: Style) {
        if (style.getSource(USER_LOCATION_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(USER_LOCATION_SOURCE_ID))
        }
        if (style.getLayer(USER_LOCATION_HALO_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(USER_LOCATION_HALO_LAYER_ID, USER_LOCATION_SOURCE_ID)
                    .withProperties(
                        circleRadius(16f),
                        circleColor(USER_LOCATION_COLOR),
                        circleOpacity(0.2f)
                    )
            )
        }
        if (style.getLayer(USER_LOCATION_DOT_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(USER_LOCATION_DOT_LAYER_ID, USER_LOCATION_SOURCE_ID)
                    .withProperties(
                        circleRadius(8f),
                        circleColor(USER_LOCATION_COLOR),
                        circleStrokeColor(USER_LOCATION_STROKE_COLOR),
                        circleStrokeWidth(3f)
                    )
            )
        }
    }

    private fun renderUserLocation() {
        val source = style?.getSourceAs<GeoJsonSource>(USER_LOCATION_SOURCE_ID) ?: return
        val location = userLocation
        if (location == null) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        } else {
            source.setGeoJson(Point.fromLngLat(location.longitude, location.latitude))
        }
    }

    private fun startMap() {
        if (!isDisposed && !isStarted) {
            mapView.onStart()
            isStarted = true
        }
    }

    private fun resumeMap() {
        if (!isDisposed && !isResumed) {
            if (!isStarted) startMap()
            mapView.onResume()
            isResumed = true
        }
    }

    private fun pauseMap() {
        if (!isDisposed && isResumed) {
            mapView.onPause()
            isResumed = false
        }
    }

    private fun stopMap() {
        if (!isDisposed && isStarted) {
            if (isResumed) pauseMap()
            mapView.onStop()
            isStarted = false
        }
    }

    private companion object {
        const val USER_LOCATION_SOURCE_ID = "pest-map-user-location-source"
        const val USER_LOCATION_HALO_LAYER_ID = "pest-map-user-location-halo"
        const val USER_LOCATION_DOT_LAYER_ID = "pest-map-user-location-dot"
        const val USER_LOCATION_COLOR = "#1565C0"
        const val USER_LOCATION_STROKE_COLOR = "#FFFFFF"
        const val CAMERA_ANIMATION_DURATION_MS = 650
    }
}
