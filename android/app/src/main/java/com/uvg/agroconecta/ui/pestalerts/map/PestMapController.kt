package com.uvg.agroconecta.ui.pestalerts.map

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

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
    fun dispose()
}

internal fun interface PestMapHostFactory {
    fun create(
        context: Context,
        savedState: Bundle?,
        cameraState: PestMapCameraState,
        onLoadStateChanged: (PestMapLoadState) -> Unit,
        onCameraChanged: (PestMapCameraState) -> Unit
    ): PestMapHost
}

internal val LocalPestMapHostFactory = staticCompositionLocalOf {
    PestMapHostFactory { context, savedState, cameraState, onLoadStateChanged, onCameraChanged ->
        PestMapController(
            context = context,
            savedState = savedState,
            cameraState = cameraState,
            onLoadStateChanged = onLoadStateChanged,
            onCameraChanged = onCameraChanged
        )
    }
}

internal class PestMapController(
    context: Context,
    savedState: Bundle?,
    cameraState: PestMapCameraState,
    private val onLoadStateChanged: (PestMapLoadState) -> Unit,
    private val onCameraChanged: (PestMapCameraState) -> Unit
) : PestMapHost, DefaultLifecycleObserver {

    private val mapView: MapView
    private var map: MapLibreMap? = null
    private var lifecycle: Lifecycle? = null
    private var isStarted = false
    private var isResumed = false
    private var isDisposed = false
    private val mapViewState = savedState ?: Bundle()

    private val cameraIdleListener = MapLibreMap.OnCameraIdleListener {
        map?.cameraPosition?.let { position ->
            onCameraChanged(PestMapCameraState.from(position))
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

    override fun dispose() {
        if (isDisposed) return
        lifecycle?.removeObserver(this)
        lifecycle = null
        saveState(mapViewState)
        pauseMap()
        stopMap()
        map?.removeOnCameraIdleListener(cameraIdleListener)
        map = null
        mapView.removeOnDidFailLoadingMapListener(mapLoadFailureListener)
        mapView.onDestroy()
        isDisposed = true
    }

    private fun loadStyle() {
        val readyMap = map ?: return
        onLoadStateChanged(PestMapLoadState.Loading)
        readyMap.setStyle(PestMapConfig.styleUrl) {
            if (!isDisposed) {
                onLoadStateChanged(PestMapLoadState.Ready)
            }
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
}
