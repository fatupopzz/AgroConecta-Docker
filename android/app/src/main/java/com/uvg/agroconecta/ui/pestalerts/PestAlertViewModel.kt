package com.uvg.agroconecta.ui.pestalerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.repository.PestAlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PestAlertLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = PestAlertRepository.DEFAULT_RADIUS_KM
)

data class PestAlertUiState(
    val alerts: List<PestAlert> = emptyList(),
    val location: PestAlertLocation? = null,
    val selectedAlert: PestAlert? = null,
    val suggestedProducts: List<PestSuggestedProduct> = emptyList(),
    val isLoadingAlerts: Boolean = false,
    val isLoadingSuggestions: Boolean = false,
    val alertsErrorMessage: String? = null,
    val detailErrorMessage: String? = null
)

@HiltViewModel
class PestAlertViewModel @Inject constructor(
    private val repository: PestAlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PestAlertUiState())
    val uiState: StateFlow<PestAlertUiState> = _uiState.asStateFlow()

    private var alertsJob: Job? = null
    private var suggestionsJob: Job? = null

    fun loadNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = PestAlertRepository.DEFAULT_RADIUS_KM
    ) {
        val location = PestAlertLocation(latitude, longitude, radiusKm)
        val validationError = location.validationError()
        if (validationError != null) {
            _uiState.update {
                it.copy(
                    isLoadingAlerts = false,
                    alertsErrorMessage = validationError
                )
            }
            return
        }

        alertsJob?.cancel()
        _uiState.update {
            it.copy(
                location = location,
                isLoadingAlerts = true,
                alertsErrorMessage = null
            )
        }
        alertsJob = viewModelScope.launch {
            runCatching {
                repository.getNearbyAlerts(latitude, longitude, radiusKm)
            }.onSuccess { alerts ->
                _uiState.update { current ->
                    if (current.location != location) {
                        current
                    } else {
                        current.copy(
                            alerts = alerts,
                            isLoadingAlerts = false,
                            alertsErrorMessage = null
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.location != location) {
                        current
                    } else {
                        current.copy(
                            isLoadingAlerts = false,
                            alertsErrorMessage = error.message
                                ?: "No se pudieron cargar las alertas cercanas"
                        )
                    }
                }
            }
        }
    }

    fun retryNearbyAlerts() {
        val location = _uiState.value.location ?: return
        loadNearbyAlerts(
            latitude = location.latitude,
            longitude = location.longitude,
            radiusKm = location.radiusKm
        )
    }

    fun selectAlert(alert: PestAlert) {
        suggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                selectedAlert = alert,
                suggestedProducts = emptyList(),
                isLoadingSuggestions = true,
                detailErrorMessage = null
            )
        }
        suggestionsJob = viewModelScope.launch {
            runCatching {
                repository.getSuggestedProducts(alert.id)
            }.onSuccess { products ->
                _uiState.update { current ->
                    if (current.selectedAlert?.id != alert.id) {
                        current
                    } else {
                        current.copy(
                            suggestedProducts = products,
                            isLoadingSuggestions = false,
                            detailErrorMessage = null
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.selectedAlert?.id != alert.id) {
                        current
                    } else {
                        current.copy(
                            isLoadingSuggestions = false,
                            detailErrorMessage = error.message
                                ?: "No se pudieron cargar los productos sugeridos"
                        )
                    }
                }
            }
        }
    }

    fun dismissAlertDetail() {
        suggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                selectedAlert = null,
                suggestedProducts = emptyList(),
                isLoadingSuggestions = false,
                detailErrorMessage = null
            )
        }
    }

    fun clearAlertsError() {
        _uiState.update { it.copy(alertsErrorMessage = null) }
    }
}

private fun PestAlertLocation.validationError(): String? = when {
    !latitude.isFinite() || latitude !in -90.0..90.0 ->
        "La latitud debe estar entre -90 y 90"

    !longitude.isFinite() || longitude !in -180.0..180.0 ->
        "La longitud debe estar entre -180 y 180"

    !radiusKm.isFinite() || radiusKm <= 0.0 || radiusKm > 100.0 ->
        "El radio debe estar entre 0 y 100 km"

    else -> null
}
