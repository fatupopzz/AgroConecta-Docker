package com.uvg.agroconecta.ui.farmlocation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.GuatemalaLocations
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.SaveFarmLocationRequest
import com.uvg.agroconecta.data.repository.FarmLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FarmLocationUiState(
    val isLoading: Boolean = true,
    val departamento: String = "",
    val municipio: String = "",
    val referencia: String = "",
    val coordinates: GeoCoordinates? = null,
    val isLocating: Boolean = false,
    val locationError: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
) {
    val municipios: List<String> get() = GuatemalaLocations.municipiosDe(departamento)
    val canSave: Boolean get() = departamento.isNotBlank() && municipio.isNotBlank() && !isSaving && !isLoading
}

@HiltViewModel
class FarmLocationViewModel @Inject constructor(
    private val repository: FarmLocationRepository,
    private val locationProvider: CurrentLocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(FarmLocationUiState())
    val uiState: StateFlow<FarmLocationUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val saved = repository.load()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        departamento = saved?.departamento.orEmpty(),
                        municipio = saved?.municipio.orEmpty(),
                        referencia = saved?.referencia.orEmpty(),
                        coordinates = saved?.coordinates
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Se deja llenar igual: si no cargó la anterior, guardar la
                // nueva la reemplaza.
                _uiState.update {
                    it.copy(isLoading = false, error = "No pudimos traer la ubicación guardada. Revisa tu señal.")
                }
            }
        }
    }

    fun onDepartamentoChange(value: String) {
        _uiState.update {
            // El municipio anterior no pertenece al nuevo departamento.
            val keepMunicipio = it.municipio in GuatemalaLocations.municipiosDe(value)
            it.copy(departamento = value, municipio = if (keepMunicipio) it.municipio else "", error = null)
        }
    }

    fun onMunicipioChange(value: String) {
        _uiState.update { it.copy(municipio = value, error = null) }
    }

    fun onReferenciaChange(value: String) {
        _uiState.update { it.copy(referencia = value.take(MAX_REFERENCE_LENGTH), error = null) }
    }

    fun useCurrentLocation() {
        _uiState.update { it.copy(isLocating = true, locationError = null) }
        viewModelScope.launch {
            val message = try {
                val coordinates = locationProvider.getCurrentCoordinates()
                if (coordinates != null) {
                    _uiState.update { it.copy(coordinates = coordinates, isLocating = false) }
                    return@launch
                }
                "No pudimos ubicarte. Revisa que el GPS esté encendido y vuelve a intentar."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                LOCATION_PERMISSION_MESSAGE
            } catch (_: Exception) {
                "No pudimos ubicarte. Vuelve a intentar en un momento."
            }
            _uiState.update { it.copy(isLocating = false, locationError = message) }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update { it.copy(isLocating = false, locationError = LOCATION_PERMISSION_MESSAGE) }
    }

    fun clearCoordinates() {
        _uiState.update { it.copy(coordinates = null, locationError = null) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(
                    SaveFarmLocationRequest(
                        departamento = state.departamento,
                        municipio = state.municipio,
                        referencia = state.referencia.trim().ifBlank { null },
                        latitude = state.coordinates?.latitude,
                        longitude = state.coordinates?.longitude
                    )
                )
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "No se pudo guardar. Revisa tu señal e inténtalo otra vez.")
                }
            }
        }
    }

    fun onSavedShown() {
        _uiState.update { it.copy(saved = false) }
    }

    private companion object {
        const val MAX_REFERENCE_LENGTH = 255
        const val LOCATION_PERMISSION_MESSAGE =
            "Sin permiso de ubicación no podemos marcar tu finca. Puedes guardar solo el municipio."
    }
}
