package com.uvg.agroconecta.ui.pestalerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.data.repository.PestAlertRepository
import com.uvg.agroconecta.ui.pestalerts.map.PestMapConfig
import com.uvg.agroconecta.ui.pestalerts.map.PestMapTrackingMode
import com.uvg.agroconecta.validation.PestAlertReportFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

data class PestAlertLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = PestAlertRepository.DEFAULT_RADIUS_KM
)

data class PestMapSearchArea(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusKm: Double
)

data class PestAlertUiState(
    val alerts: List<PestAlert> = emptyList(),
    val userLocation: PestAlertLocation? = null,
    val searchArea: PestMapSearchArea? = null,
    val cameraCenter: PestMapSearchArea? = null,
    val lastSearchArea: PestMapSearchArea? = null,
    val selectedAlertId: Int? = null,
    val selectedAlertDetails: PestAlert? = null,
    val trackingMode: PestMapTrackingMode = PestMapTrackingMode.FOLLOWING_USER,
    val suggestedProducts: List<PestSuggestedProduct> = emptyList(),
    val isLoadingAlerts: Boolean = false,
    val isLoadingSuggestions: Boolean = false,
    val alertsErrorMessage: String? = null,
    val detailErrorMessage: String? = null,
    val isReportFormVisible: Boolean = false,
    val isSelectingReportLocation: Boolean = false,
    val reportLocation: PestAlertLocation? = null,
    val reportLocationDraft: PestAlertLocation? = null,
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

val pestMapSearchRadiusOptionsKm = listOf(5.0, 10.0, 25.0, 50.0)

@HiltViewModel
class PestAlertViewModel @Inject constructor(
    private val repository: PestAlertRepository,
    private val locationProvider: CurrentLocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PestAlertUiState())
    val uiState: StateFlow<PestAlertUiState> = _uiState.asStateFlow()

    private var alertsJob: Job? = null
    private var alertDetailJob: Job? = null
    private var suggestionsJob: Job? = null
    private var locationJob: Job? = null
    private var locationTrackingJob: Job? = null
    private var radiusSearchJob: Job? = null

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
                        val location = PestAlertLocation(
                            latitude = coordinates.latitude,
                            longitude = coordinates.longitude
                        )
                        it.copy(
                            userLocation = location,
                            reportLocation = if (
                                it.isReportFormVisible && it.reportLocation == null
                            ) {
                                location
                            } else {
                                it.reportLocation
                            },
                            reportLocationDraft = if (
                                it.isReportFormVisible && it.reportLocationDraft == null
                            ) {
                                location
                            } else {
                                it.reportLocationDraft
                            },
                            isLocating = false,
                            locationErrorMessage = null,
                            reportErrorMessage = null
                        )
                    }
                    synchronizePushRegistration(
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude
                    )
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
        stopLocationTracking()
        _uiState.update {
            it.copy(
                userLocation = null,
                isLocating = false,
                locationErrorMessage = "Activa el permiso de ubicación para consultar y reportar alertas cercanas"
            )
        }
    }

    fun startLocationTracking() {
        if (locationTrackingJob?.isActive == true) return
        locationTrackingJob = viewModelScope.launch {
            locationProvider.observeCoordinates()
                .catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            locationErrorMessage = if (error is SecurityException) {
                                "Se necesita permiso de ubicación para actualizar tu posición"
                            } else {
                                error.message ?: "No se pudo actualizar tu ubicación"
                            }
                        )
                    }
                }
                .collect { coordinates ->
                    _uiState.update { current ->
                        val radius = current.userLocation?.radiusKm
                            ?: current.searchArea?.radiusKm
                            ?: PestAlertRepository.DEFAULT_RADIUS_KM
                        current.copy(
                            userLocation = PestAlertLocation(
                                latitude = coordinates.latitude,
                                longitude = coordinates.longitude,
                                radiusKm = radius
                            ),
                            reportLocation = if (
                                current.isReportFormVisible && current.reportLocation == null
                            ) {
                                PestAlertLocation(
                                    latitude = coordinates.latitude,
                                    longitude = coordinates.longitude,
                                    radiusKm = radius
                                )
                            } else {
                                current.reportLocation
                            },
                            locationErrorMessage = null
                        )
                    }
                }
        }
    }

    fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }

    fun loadNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = PestAlertRepository.DEFAULT_RADIUS_KM
    ) {
        radiusSearchJob?.cancel()
        radiusSearchJob = null
        val requestedLocation = PestAlertLocation(latitude, longitude, radiusKm)
        val validationError = requestedLocation.validationError()
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
        val requestedArea = PestMapSearchArea(
            centerLatitude = latitude,
            centerLongitude = longitude,
            radiusKm = radiusKm
        )
        _uiState.update {
            it.copy(
                searchArea = requestedArea,
                isLoadingAlerts = true,
                alertsErrorMessage = null
            )
        }
        alertsJob = viewModelScope.launch {
            runCatching {
                repository.getNearbyAlerts(latitude, longitude, radiusKm)
            }.onSuccess { alerts ->
                _uiState.update { current ->
                    if (current.searchArea != requestedArea) {
                        current
                    } else {
                        current.copy(
                            alerts = alerts,
                            lastSearchArea = requestedArea,
                            isLoadingAlerts = false,
                            alertsErrorMessage = null
                        )
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    if (current.searchArea != requestedArea) {
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
        val searchArea = _uiState.value.searchArea ?: _uiState.value.lastSearchArea ?: return
        loadNearbyAlerts(
            latitude = searchArea.centerLatitude,
            longitude = searchArea.centerLongitude,
            radiusKm = searchArea.radiusKm
        )
    }

    fun onMapMoved(center: GeoCoordinates) {
        if (!center.latitude.isFinite() || center.latitude !in -90.0..90.0) return
        if (!center.longitude.isFinite() || center.longitude !in -180.0..180.0) return

        _uiState.update { current ->
            val radiusKm = current.cameraCenter?.radiusKm
                ?: current.lastSearchArea?.radiusKm
                ?: current.searchArea?.radiusKm
                ?: PestAlertRepository.DEFAULT_RADIUS_KM
            val mapCenter = PestMapSearchArea(
                centerLatitude = center.latitude,
                centerLongitude = center.longitude,
                radiusKm = radiusKm
            )
            current.copy(
                cameraCenter = mapCenter,
                reportLocationDraft = if (current.isSelectingReportLocation) {
                    PestAlertLocation(
                        latitude = center.latitude,
                        longitude = center.longitude,
                        radiusKm = radiusKm
                    )
                } else {
                    current.reportLocationDraft
                }
            )
        }
    }

    fun searchVisibleArea() {
        val area = _uiState.value.cameraCenter ?: return
        loadNearbyAlerts(
            latitude = area.centerLatitude,
            longitude = area.centerLongitude,
            radiusKm = area.radiusKm
        )
    }

    fun changeSearchRadius(radiusKm: Double) {
        if (radiusKm !in pestMapSearchRadiusOptionsKm) return
        val state = _uiState.value
        val currentArea = state.cameraCenter
            ?: state.lastSearchArea
            ?: state.searchArea
            ?: state.userLocation?.let { location ->
                PestMapSearchArea(location.latitude, location.longitude, location.radiusKm)
            }
            ?: PestMapSearchArea(
                centerLatitude = PestMapConfig.INITIAL_LATITUDE,
                centerLongitude = PestMapConfig.INITIAL_LONGITUDE,
                radiusKm = PestAlertRepository.DEFAULT_RADIUS_KM
            )
        if (currentArea.radiusKm == radiusKm) return

        val updatedArea = currentArea.copy(radiusKm = radiusKm)
        _uiState.update { it.copy(cameraCenter = updatedArea) }
        radiusSearchJob?.cancel()
        radiusSearchJob = viewModelScope.launch {
            delay(SEARCH_RADIUS_DEBOUNCE_MS)
            radiusSearchJob = null
            loadNearbyAlerts(
                latitude = updatedArea.centerLatitude,
                longitude = updatedArea.centerLongitude,
                radiusKm = updatedArea.radiusKm
            )
        }
    }

    fun selectAlert(alertId: Int?) {
        if (alertId == null) {
            suggestionsJob?.cancel()
            _uiState.update {
                it.copy(
                    selectedAlertId = null,
                    selectedAlertDetails = null,
                    suggestedProducts = emptyList(),
                    isLoadingSuggestions = false,
                    detailErrorMessage = null
                )
            }
            return
        }
        if (_uiState.value.alerts.none { it.id == alertId }) return
        _uiState.update { it.copy(selectedAlertId = alertId) }
    }

    fun openAlertDetails(alertId: Int) {
        val alert = _uiState.value.alerts.firstOrNull { it.id == alertId } ?: return
        selectAlertDetails(alert)
    }

    private fun selectAlertDetails(alert: PestAlert) {
        suggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                selectedAlertId = alert.id,
                selectedAlertDetails = alert,
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
                    if (current.selectedAlertId != alert.id) {
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
                    if (current.selectedAlertId != alert.id) {
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

    fun openAlert(alertId: Int) {
        if (alertId <= 0) return
        alertDetailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedAlertId = alertId,
                selectedAlertDetails = null,
                suggestedProducts = emptyList(),
                detailErrorMessage = null
            )
        }
        alertDetailJob = viewModelScope.launch {
            runCatching {
                repository.getAlert(alertId)
            }.onSuccess(::selectAlertDetails)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            alertsErrorMessage = error.message
                                ?: "No se pudo abrir la alerta"
                        )
                    }
                }
        }
    }

    fun dismissAlertDetail() {
        suggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                selectedAlertDetails = null,
                suggestedProducts = emptyList(),
                isLoadingSuggestions = false,
                detailErrorMessage = null
            )
        }
    }

    fun retrySuggestedProducts() {
        val selectedAlert = _uiState.value.selectedAlertDetails ?: return
        selectAlertDetails(selectedAlert)
    }

    fun clearAlertsError() {
        _uiState.update { it.copy(alertsErrorMessage = null) }
    }

    fun setViewMode(viewMode: PestAlertViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
    }

    fun setTrackingMode(trackingMode: PestMapTrackingMode) {
        _uiState.update { it.copy(trackingMode = trackingMode) }
    }

    fun centerOnUser() {
        _uiState.update { it.copy(trackingMode = PestMapTrackingMode.FOLLOWING_USER) }
    }

    fun openReportForm() {
        if (_uiState.value.isSubmittingReport) return
        _uiState.update {
            it.copy(
                isReportFormVisible = true,
                isSelectingReportLocation = false,
                reportLocation = it.userLocation,
                reportLocationDraft = it.userLocation,
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
                isSelectingReportLocation = false,
                reportLocationDraft = null,
                reportErrorMessage = null
            )
        }
    }

    fun openReportLocationPicker() {
        if (_uiState.value.isSubmittingReport) return
        _uiState.update { current ->
            val startingLocation = current.reportLocation
                ?: current.userLocation
                ?: current.cameraCenter?.toPestAlertLocation()
                ?: current.lastSearchArea?.toPestAlertLocation()
                ?: PestAlertLocation(
                    latitude = PestMapConfig.INITIAL_LATITUDE,
                    longitude = PestMapConfig.INITIAL_LONGITUDE
                )
            current.copy(
                isSelectingReportLocation = true,
                reportLocationDraft = startingLocation,
                viewMode = PestAlertViewMode.MAP,
                trackingMode = PestMapTrackingMode.FREE_EXPLORATION,
                reportErrorMessage = null
            )
        }
    }

    fun confirmReportLocation() {
        _uiState.update { current ->
            current.copy(
                isSelectingReportLocation = false,
                reportLocation = current.reportLocationDraft,
                reportErrorMessage = null
            )
        }
    }

    fun cancelReportLocationPicker() {
        _uiState.update { current ->
            current.copy(
                isSelectingReportLocation = false,
                reportLocationDraft = current.reportLocation
            )
        }
    }

    fun useCurrentLocationForReport() {
        val location = _uiState.value.userLocation ?: return
        _uiState.update {
            it.copy(
                reportLocation = location,
                reportLocationDraft = location,
                trackingMode = PestMapTrackingMode.FOLLOWING_USER,
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
        val location = state.reportLocation
        val form = state.reportForm
        val validationError = PestAlertReportFormValidator.errorFor(
            hasPestType = form.selectedPestType != null,
            crop = form.selectedCrop,
            hasLocation = location != null
        )
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
                        isSelectingReportLocation = false,
                        reportLocation = null,
                        reportLocationDraft = null,
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

    private fun synchronizePushRegistration(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                repository.syncPushRegistration(latitude, longitude)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // El registro push es best-effort y no debe bloquear la pantalla.
            }
        }
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

internal val PestAlertUiState.shouldShowVisibleAreaSearch: Boolean
    get() {
        val camera = cameraCenter ?: return false
        val previous = lastSearchArea ?: return true
        return camera.radiusKm != previous.radiusKm ||
            camera.distanceMetersTo(previous) >=
            PestMapConfig.SEARCH_CENTER_CHANGE_THRESHOLD_METERS
    }

internal val PestAlertUiState.activeSearchRadiusKm: Double
    get() = cameraCenter?.radiusKm
        ?: lastSearchArea?.radiusKm
        ?: searchArea?.radiusKm
        ?: userLocation?.radiusKm
        ?: PestAlertRepository.DEFAULT_RADIUS_KM

private fun PestMapSearchArea.distanceMetersTo(other: PestMapSearchArea): Double {
    val latitudeDelta = Math.toRadians(other.centerLatitude - centerLatitude)
    val longitudeDelta = Math.toRadians(other.centerLongitude - centerLongitude)
    val firstLatitude = Math.toRadians(centerLatitude)
    val secondLatitude = Math.toRadians(other.centerLatitude)
    val haversine = (sin(latitudeDelta / 2.0).let { it * it } +
        cos(firstLatitude) * cos(secondLatitude) *
        sin(longitudeDelta / 2.0).let { it * it })
        .coerceIn(0.0, 1.0)
    val angularDistance = 2.0 * atan2(sqrt(haversine), sqrt(1.0 - haversine))
    return EARTH_RADIUS_METERS * angularDistance
}

private fun PestMapSearchArea.toPestAlertLocation(): PestAlertLocation = PestAlertLocation(
    latitude = centerLatitude,
    longitude = centerLongitude,
    radiusKm = radiusKm
)

private const val EARTH_RADIUS_METERS = 6_371_000.0
internal const val SEARCH_RADIUS_DEBOUNCE_MS = 350L
