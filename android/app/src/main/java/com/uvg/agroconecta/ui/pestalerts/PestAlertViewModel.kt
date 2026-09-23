package com.uvg.agroconecta.ui.pestalerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.PestType
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
    val detailErrorMessage: String? = null,
    val isReportFormVisible: Boolean = false,
    val reportForm: PestReportFormState = PestReportFormState(),
    val isSubmittingReport: Boolean = false,
    val reportErrorMessage: String? = null,
    val reportSuccessMessage: String? = null,
    val isLocating: Boolean = false,
    val locationErrorMessage: String? = null,
    val viewMode: PestAlertViewMode = PestAlertViewMode.LIST
)

enum class PestAlertViewMode {
    LIST,
    MAP
}

data class PestReportFormState(
    val selectedPestType: PestType? = null,
    val selectedCrop: String = "",
    val description: String = ""
)

val pestAlertCrops = listOf(
    "Maíz",
    "Frijol",
    "Café",
    "Tomate",
    "Papa",
    "Cardamomo",
    "Banano",
    "Hortalizas",
    "Otro"
)

@HiltViewModel
class PestAlertViewModel @Inject constructor(
    private val repository: PestAlertRepository,
    private val locationProvider: CurrentLocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PestAlertUiState())
    val uiState: StateFlow<PestAlertUiState> = _uiState.asStateFlow()

    private var alertsJob: Job? = null
    private var suggestionsJob: Job? = null
    private var locationJob: Job? = null

    fun refreshLocation() {
        locationJob?.cancel()
        _uiState.update {
            it.copy(
                isLocating = true,
                locationErrorMessage = null
            )
        }
        locationJob = viewModelScope.launch {
            runCatching {
                locationProvider.getCurrentCoordinates()
            }.onSuccess { coordinates ->
                if (coordinates == null) {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationErrorMessage = "No se pudo determinar tu ubicación. Verifica que el GPS esté activo"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            location = PestAlertLocation(
                                latitude = coordinates.latitude,
                                longitude = coordinates.longitude
                            ),
                            isLocating = false,
                            locationErrorMessage = null,
                            reportErrorMessage = null
                        )
                    }
                    loadNearbyAlerts(
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                val message = if (error is SecurityException) {
                    "Se necesita permiso de ubicación para mostrar alertas cercanas"
                } else {
                    error.message ?: "No se pudo obtener tu ubicación"
                }
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        locationErrorMessage = message
                    )
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        locationJob?.cancel()
        _uiState.update {
            it.copy(
                location = null,
                isLocating = false,
                locationErrorMessage = "Activa el permiso de ubicación para consultar y reportar alertas cercanas"
            )
        }
    }

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

    fun retrySuggestedProducts() {
        val selectedAlert = _uiState.value.selectedAlert ?: return
        selectAlert(selectedAlert)
    }

    fun clearAlertsError() {
        _uiState.update { it.copy(alertsErrorMessage = null) }
    }

    fun setViewMode(viewMode: PestAlertViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
    }

    fun openReportForm() {
        if (_uiState.value.isSubmittingReport) return
        _uiState.update {
            it.copy(
                isReportFormVisible = true,
                reportForm = PestReportFormState(),
                reportErrorMessage = null,
                reportSuccessMessage = null
            )
        }
    }

    fun dismissReportForm() {
        if (_uiState.value.isSubmittingReport) return
        _uiState.update {
            it.copy(
                isReportFormVisible = false,
                reportErrorMessage = null
            )
        }
    }

    fun selectReportPestType(pestType: PestType) {
        _uiState.update {
            it.copy(
                reportForm = it.reportForm.copy(selectedPestType = pestType),
                reportErrorMessage = null
            )
        }
    }

    fun selectReportCrop(crop: String) {
        _uiState.update {
            it.copy(
                reportForm = it.reportForm.copy(selectedCrop = crop),
                reportErrorMessage = null
            )
        }
    }

    fun updateReportDescription(description: String) {
        _uiState.update {
            it.copy(
                reportForm = it.reportForm.copy(description = description.take(MAX_DESCRIPTION_LENGTH)),
                reportErrorMessage = null
            )
        }
    }

    fun submitPestReport() {
        if (_uiState.value.isSubmittingReport) return
        val state = _uiState.value
        val location = state.location
        val form = state.reportForm
        val validationError = when {
            form.selectedPestType == null -> "Selecciona el tipo de plaga"
            form.selectedCrop.isBlank() -> "Selecciona el cultivo afectado"
            location == null -> "No se pudo obtener la ubicación del reporte"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(reportErrorMessage = validationError) }
            return
        }

        val reportLocation = checkNotNull(location)
        val reportPestType = checkNotNull(form.selectedPestType)
        val request = PestAlertReportRequest(
            pestType = reportPestType.apiValue,
            cultivo = form.selectedCrop.trim(),
            latitud = reportLocation.latitude,
            longitud = reportLocation.longitude,
            descripcion = form.description.trim().ifBlank { null }
        )

        _uiState.update {
            it.copy(
                isSubmittingReport = true,
                reportErrorMessage = null,
                reportSuccessMessage = null
            )
        }
        viewModelScope.launch {
            runCatching {
                repository.reportPest(request)
            }.onSuccess { createdAlert ->
                _uiState.update { current ->
                    current.copy(
                        alerts = (listOf(createdAlert) + current.alerts)
                            .distinctBy(PestAlert::id),
                        isReportFormVisible = false,
                        reportForm = PestReportFormState(),
                        isSubmittingReport = false,
                        reportErrorMessage = null,
                        reportSuccessMessage = "Alerta reportada correctamente"
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isSubmittingReport = false,
                        reportErrorMessage = error.message
                            ?: "No se pudo reportar la plaga"
                    )
                }
            }
        }
    }

    fun clearReportSuccess() {
        _uiState.update { it.copy(reportSuccessMessage = null) }
    }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 500
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
