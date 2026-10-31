package com.uvg.agroconecta.ui.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Estado de conexión para el aviso global de la app. Solo traduce el monitor
 * existente a algo que la UI pueda observar; no decide nada de negocio.
 *
 * Arranca en "en línea" para que el aviso no parpadee al abrir la app
 * mientras el monitor entrega su primer valor.
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    monitor: ConnectivityMonitor
) : ViewModel() {

    val isOffline: StateFlow<Boolean> = monitor.status
        .map { online -> !online }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = false
        )

    private companion object {
        // Margen para que una rotación de pantalla no reinicie el callback de red.
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
