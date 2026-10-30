package com.uvg.agroconecta.ui.pestalerts.map

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.uvg.agroconecta.R
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
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
    fun updateSearchRadius(searchRadius: PestMapSearchRadius?)
    fun updatePestMarkers(
        markers: List<PestMapMarker>,
        selectedAlertId: Int?,
        onMarkerSelected: (Int) -> Unit,
        onSelectionCleared: () -> Unit
    )
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

    private val appContext = context.applicationContext
    private val mapView: MapView
    private val mapViewState = savedState ?: Bundle()
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var lifecycle: Lifecycle? = null
    private var userLocation: PestMapCoordinate? = null
    private var searchRadius: PestMapSearchRadius? = null
    private var pestMarkersById: Map<Int, PestMapMarker> = emptyMap()
    private var selectedPestMarkerId: Int? = null
    private var onPestMarkerSelected: (Int) -> Unit = {}
    private var onPestMarkerSelectionCleared: () -> Unit = {}
    private var hasReceivedUserLocation = false
    private var hasReceivedPestMarkers = false
    private var hasAdjustedInitialCamera = mapViewState.getBoolean(INITIAL_CAMERA_ADJUSTED_KEY)
    private var isInitialCameraAdjustmentScheduled = false
    private var shouldCenterOnUser = false
    private var isStarted = false
    private var isResumed = false
    private var isDisposed = false

    private val initialCameraLayoutListener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (view.width > 0 && view.height > 0) {
                view.removeOnLayoutChangeListener(this)
                isInitialCameraAdjustmentScheduled = false
                adjustInitialCameraIfReady()
            }
        }
    }

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

    private val mapClickListener = MapLibreMap.OnMapClickListener { coordinate ->
        val currentMap = map ?: return@OnMapClickListener false
        val renderedFeatures = currentMap.queryRenderedFeatures(
            currentMap.projection.toScreenLocation(coordinate),
            PEST_MARKER_CLUSTER_LAYER_ID,
            PEST_MARKER_CIRCLE_LAYER_ID
        )
        val clusterFeature = renderedFeatures.firstOrNull { feature ->
            feature.hasProperty(POINT_COUNT_PROPERTY)
        }
        if (clusterFeature != null) {
            clearPestMarkerSelection()
            shouldCenterOnUser = false
            onTrackingModeChanged(PestMapTrackingMode.FREE_EXPLORATION)
            expandCluster(clusterFeature)
            return@OnMapClickListener true
        }
        val feature = renderedFeatures.firstOrNull()
        val alertId = feature
            ?.getNumberProperty(PEST_ALERT_ID_PROPERTY)
            ?.toInt()
        if (alertId == null || pestMarkersById[alertId] == null) {
            clearPestMarkerSelection()
            false
        } else {
            selectedPestMarkerId = alertId
            renderSelectedPestMarker()
            shouldCenterOnUser = false
            onTrackingModeChanged(PestMapTrackingMode.FREE_EXPLORATION)
            centerOnPestMarker(pestMarkersById.getValue(alertId))
            onPestMarkerSelected(alertId)
            true
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
                readyMap.addOnMapClickListener(mapClickListener)
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
            outState.putBoolean(INITIAL_CAMERA_ADJUSTED_KEY, hasAdjustedInitialCamera)
        }
    }

    override fun retryStyle() {
        if (!isDisposed) {
            loadStyle()
        }
    }

    override fun updateUserLocation(location: PestMapCoordinate?, centerCamera: Boolean) {
        hasReceivedUserLocation = true
        userLocation = location?.takeIf(PestMapCoordinate::isValid)
        renderUserLocation()
        if (hasAdjustedInitialCamera) {
            shouldCenterOnUser = centerCamera && userLocation != null
            if (shouldCenterOnUser) {
                centerOnUser()
            }
        } else {
            shouldCenterOnUser = false
            adjustInitialCameraIfReady()
        }
    }

    override fun updateSearchRadius(searchRadius: PestMapSearchRadius?) {
        val validSearchRadius = searchRadius?.takeIf(PestMapSearchRadius::isValid)
        if (validSearchRadius == this.searchRadius) return
        val shouldFitUpdatedRadius = this.searchRadius?.let { previous ->
            validSearchRadius != null &&
                previous.center == validSearchRadius.center &&
                previous.radiusKm != validSearchRadius.radiusKm
        } == true
        this.searchRadius = validSearchRadius
        renderSearchRadius()
        if (shouldFitUpdatedRadius) {
            validSearchRadius?.let(::fitSearchRadius)
        }
    }

    override fun updatePestMarkers(
        markers: List<PestMapMarker>,
        selectedAlertId: Int?,
        onMarkerSelected: (Int) -> Unit,
        onSelectionCleared: () -> Unit
    ) {
        onPestMarkerSelected = onMarkerSelected
        onPestMarkerSelectionCleared = onSelectionCleared
        hasReceivedPestMarkers = true
        val updatedMarkers = markers.associateBy(PestMapMarker::alertId)
        val updatedSelection = selectedAlertId?.takeIf(updatedMarkers::containsKey)
        if (updatedMarkers != pestMarkersById) {
            pestMarkersById = updatedMarkers
            renderPestMarkers()
            renderSelectedPestMarker()
        }
        if (updatedSelection != selectedPestMarkerId) {
            selectedPestMarkerId = updatedSelection
            renderSelectedPestMarker()
        }
        adjustInitialCameraIfReady()
    }

    private fun clearPestMarkerSelection() {
        if (selectedPestMarkerId == null) return
        selectedPestMarkerId = null
        renderSelectedPestMarker()
        onPestMarkerSelectionCleared()
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
        map?.removeOnMapClickListener(mapClickListener)
        mapView.removeOnLayoutChangeListener(initialCameraLayoutListener)
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
                installSearchRadiusLayers(loadedStyle)
                installPestMarkerLayers(loadedStyle)
                installUserLocationLayers(loadedStyle)
                renderPestMarkers()
                renderSelectedPestMarker()
                renderSearchRadius()
                renderUserLocation()
                adjustInitialCameraIfReady()
                if (hasAdjustedInitialCamera && shouldCenterOnUser) {
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

    private fun installPestMarkerLayers(style: Style) {
        if (style.getSource(PEST_MARKER_SOURCE_ID) == null) {
            style.addSource(
                GeoJsonSource(
                    PEST_MARKER_SOURCE_ID,
                    GeoJsonOptions()
                        .withCluster(true)
                        .withClusterMaxZoom(PEST_MARKER_CLUSTER_MAX_ZOOM)
                        .withClusterRadius(PEST_MARKER_CLUSTER_RADIUS_PIXELS)
                        .withClusterMinPoints(PEST_MARKER_CLUSTER_MIN_POINTS)
                )
            )
        }
        if (style.getSource(SELECTED_PEST_MARKER_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(SELECTED_PEST_MARKER_SOURCE_ID))
        }
        if (style.getLayer(SELECTED_PEST_MARKER_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(SELECTED_PEST_MARKER_LAYER_ID, SELECTED_PEST_MARKER_SOURCE_ID)
                    .withProperties(
                        circleRadius(23f),
                        circleColor(SELECTED_PEST_MARKER_COLOR),
                        circleOpacity(0.32f),
                        circleStrokeColor(SELECTED_PEST_MARKER_COLOR),
                        circleStrokeWidth(3f)
                    )
            )
        }
        if (style.getLayer(PEST_MARKER_CLUSTER_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(PEST_MARKER_CLUSTER_LAYER_ID, PEST_MARKER_SOURCE_ID)
                    .withFilter(Expression.has(POINT_COUNT_PROPERTY))
                    .withProperties(
                        circleRadius(22f),
                        circleColor(PEST_MARKER_CLUSTER_COLOR),
                        circleOpacity(0.9f),
                        circleStrokeColor(PEST_MARKER_STROKE_COLOR),
                        circleStrokeWidth(3f)
                    )
            )
        }
        if (style.getLayer(PEST_MARKER_CLUSTER_COUNT_LAYER_ID) == null) {
            style.addLayer(
                SymbolLayer(PEST_MARKER_CLUSTER_COUNT_LAYER_ID, PEST_MARKER_SOURCE_ID)
                    .withFilter(Expression.has(POINT_COUNT_PROPERTY))
                    .withProperties(
                        textField(Expression.get(POINT_COUNT_ABBREVIATED_PROPERTY)),
                        textSize(13f),
                        textColor(PEST_MARKER_STROKE_COLOR),
                        textAllowOverlap(true)
                    )
            )
        }
        if (style.getLayer(PEST_MARKER_CIRCLE_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(PEST_MARKER_CIRCLE_LAYER_ID, PEST_MARKER_SOURCE_ID)
                    .withFilter(Expression.not(Expression.has(POINT_COUNT_PROPERTY)))
                    .withProperties(
                        circleRadius(16f),
                        circleColor(PEST_MARKER_COLOR),
                        circleStrokeColor(PEST_MARKER_STROKE_COLOR),
                        circleStrokeWidth(3f)
                    )
            )
        }
        if (style.getImage(PEST_MARKER_ICON_ID) == null) {
            appContext.getDrawable(R.drawable.ic_pest_map_bug)?.let { drawable ->
                style.addImage(PEST_MARKER_ICON_ID, drawable)
            }
        }
        if (style.getLayer(PEST_MARKER_ICON_LAYER_ID) == null) {
            style.addLayer(
                SymbolLayer(PEST_MARKER_ICON_LAYER_ID, PEST_MARKER_SOURCE_ID)
                    .withFilter(Expression.not(Expression.has(POINT_COUNT_PROPERTY)))
                    .withProperties(
                        iconImage(PEST_MARKER_ICON_ID),
                        iconSize(0.82f),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)
                    )
            )
        }
    }

    private fun installSearchRadiusLayers(style: Style) {
        if (style.getSource(SEARCH_RADIUS_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(SEARCH_RADIUS_SOURCE_ID))
        }
        if (style.getLayer(SEARCH_RADIUS_FILL_LAYER_ID) == null) {
            style.addLayer(
                FillLayer(SEARCH_RADIUS_FILL_LAYER_ID, SEARCH_RADIUS_SOURCE_ID)
                    .withProperties(
                        fillColor(SEARCH_RADIUS_COLOR),
                        fillOpacity(SEARCH_RADIUS_FILL_OPACITY)
                    )
            )
        }
        if (style.getLayer(SEARCH_RADIUS_BORDER_LAYER_ID) == null) {
            style.addLayer(
                LineLayer(SEARCH_RADIUS_BORDER_LAYER_ID, SEARCH_RADIUS_SOURCE_ID)
                    .withProperties(
                        lineColor(SEARCH_RADIUS_COLOR),
                        lineOpacity(SEARCH_RADIUS_BORDER_OPACITY),
                        lineWidth(2.5f)
                    )
            )
        }
    }

    private fun renderSearchRadius() {
        val source = style?.getSourceAs<GeoJsonSource>(SEARCH_RADIUS_SOURCE_ID) ?: return
        val ring = searchRadius?.polygonRing().orEmpty()
        if (ring.isEmpty()) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        } else {
            source.setGeoJson(
                Polygon.fromLngLats(
                    listOf(
                        ring.map { coordinate ->
                            Point.fromLngLat(coordinate.longitude, coordinate.latitude)
                        }
                    )
                )
            )
        }
    }

    private fun fitSearchRadius(searchRadius: PestMapSearchRadius) {
        val currentMap = map ?: return
        if (mapView.width == 0 || mapView.height == 0) return
        val ring = searchRadius.polygonRing()
        if (ring.isEmpty()) return

        val bounds = LatLngBounds.Builder()
            .includes(
                ring.map { coordinate ->
                    LatLng(coordinate.latitude, coordinate.longitude)
                }
            )
            .build()
        currentMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, initialCameraPaddingPixels()),
            CAMERA_ANIMATION_DURATION_MS
        )
    }

    private fun renderPestMarkers() {
        val source = style?.getSourceAs<GeoJsonSource>(PEST_MARKER_SOURCE_ID) ?: return
        val features = pestMarkersById.values
            .sortedBy(PestMapMarker::alertId)
            .map { marker ->
                Feature.fromGeometry(Point.fromLngLat(marker.longitude, marker.latitude)).apply {
                    addNumberProperty(PEST_ALERT_ID_PROPERTY, marker.alertId)
                    addStringProperty(PEST_TYPE_PROPERTY, marker.pestType)
                    addStringProperty(PEST_CROP_PROPERTY, marker.crop)
                }
            }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun renderSelectedPestMarker() {
        val source = style?.getSourceAs<GeoJsonSource>(SELECTED_PEST_MARKER_SOURCE_ID) ?: return
        val marker = selectedPestMarkerId?.let(pestMarkersById::get)
        if (marker == null) {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        } else {
            source.setGeoJson(Point.fromLngLat(marker.longitude, marker.latitude))
        }
    }

    private fun expandCluster(cluster: Feature) {
        val currentMap = map ?: return
        val source = style?.getSourceAs<GeoJsonSource>(PEST_MARKER_SOURCE_ID) ?: return
        val point = cluster.geometry() as? Point ?: return
        val targetPosition = CameraPosition.Builder(currentMap.cameraPosition)
            .target(LatLng(point.latitude(), point.longitude()))
            .zoom(
                source.getClusterExpansionZoom(cluster)
                    .toDouble()
                    .coerceAtMost(PestMapConfig.MAX_ZOOM)
            )
            .build()
        currentMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(targetPosition),
            CAMERA_ANIMATION_DURATION_MS
        )
    }

    private fun adjustInitialCameraIfReady() {
        if (isDisposed ||
            hasAdjustedInitialCamera ||
            !hasReceivedUserLocation ||
            !hasReceivedPestMarkers ||
            map == null ||
            style == null
        ) {
            return
        }
        if (mapView.width == 0 || mapView.height == 0) {
            if (!isInitialCameraAdjustmentScheduled) {
                isInitialCameraAdjustmentScheduled = true
                mapView.addOnLayoutChangeListener(initialCameraLayoutListener)
            }
            return
        }

        val currentMap = map ?: return
        val update = when (
            val plan = buildInitialCameraPlan(userLocation, pestMarkersById.values)
        ) {
            PestMapInitialCameraPlan.DefaultLocation -> CameraUpdateFactory.newLatLngZoom(
                LatLng(PestMapConfig.INITIAL_LATITUDE, PestMapConfig.INITIAL_LONGITUDE),
                PestMapConfig.INITIAL_ZOOM
            )

            is PestMapInitialCameraPlan.Center -> CameraUpdateFactory.newLatLngZoom(
                LatLng(plan.coordinate.latitude, plan.coordinate.longitude),
                PestMapConfig.INITIAL_ZOOM
            )

            is PestMapInitialCameraPlan.Bounds -> CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder()
                    .includes(
                        plan.coordinates.map { coordinate ->
                            LatLng(coordinate.latitude, coordinate.longitude)
                        }
                    )
                    .build(),
                initialCameraPaddingPixels()
            )
        }

        currentMap.moveCamera(update)
        hasAdjustedInitialCamera = true
        shouldCenterOnUser = false
        mapViewState.putBoolean(INITIAL_CAMERA_ADJUSTED_KEY, true)
    }

    private fun centerOnPestMarker(marker: PestMapMarker) {
        val currentMap = map ?: return
        val currentPosition = currentMap.cameraPosition
        val targetPosition = CameraPosition.Builder(currentPosition)
            .target(LatLng(marker.latitude, marker.longitude))
            .zoom(max(currentPosition.zoom, PestMapConfig.SELECTED_ALERT_ZOOM))
            .build()
        currentMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(targetPosition),
            CAMERA_ANIMATION_DURATION_MS
        )
    }

    private fun initialCameraPaddingPixels(): Int =
        (PestMapConfig.INITIAL_CAMERA_PADDING_DP * appContext.resources.displayMetrics.density)
            .toInt()

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
        const val SEARCH_RADIUS_SOURCE_ID = "pest-map-search-radius-source"
        const val SEARCH_RADIUS_FILL_LAYER_ID = "pest-map-search-radius-fill"
        const val SEARCH_RADIUS_BORDER_LAYER_ID = "pest-map-search-radius-border"
        const val SEARCH_RADIUS_COLOR = "#2E7D32"
        const val SEARCH_RADIUS_FILL_OPACITY = 0.16f
        const val SEARCH_RADIUS_BORDER_OPACITY = 0.85f
        const val PEST_MARKER_SOURCE_ID = "pest-map-alert-source"
        const val SELECTED_PEST_MARKER_SOURCE_ID = "pest-map-selected-alert-source"
        const val SELECTED_PEST_MARKER_LAYER_ID = "pest-map-selected-alert"
        const val PEST_MARKER_CLUSTER_LAYER_ID = "pest-map-alert-cluster"
        const val PEST_MARKER_CLUSTER_COUNT_LAYER_ID = "pest-map-alert-cluster-count"
        const val PEST_MARKER_CIRCLE_LAYER_ID = "pest-map-alert-circle"
        const val PEST_MARKER_ICON_LAYER_ID = "pest-map-alert-icon"
        const val PEST_MARKER_ICON_ID = "pest-map-bug-icon"
        const val PEST_ALERT_ID_PROPERTY = "alertId"
        const val PEST_TYPE_PROPERTY = "pestType"
        const val PEST_CROP_PROPERTY = "crop"
        const val POINT_COUNT_PROPERTY = "point_count"
        const val POINT_COUNT_ABBREVIATED_PROPERTY = "point_count_abbreviated"
        const val PEST_MARKER_COLOR = "#C62828"
        const val PEST_MARKER_CLUSTER_COLOR = "#AD342C"
        const val PEST_MARKER_STROKE_COLOR = "#FFFFFF"
        const val SELECTED_PEST_MARKER_COLOR = "#FFB300"
        const val PEST_MARKER_CLUSTER_MAX_ZOOM = 14
        const val PEST_MARKER_CLUSTER_RADIUS_PIXELS = 52
        const val PEST_MARKER_CLUSTER_MIN_POINTS = 4
        const val INITIAL_CAMERA_ADJUSTED_KEY = "pest-map-initial-camera-adjusted"
        const val CAMERA_ANIMATION_DURATION_MS = 650
    }
}
