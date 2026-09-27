package com.uvg.agroconecta.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.FarmerDashboardResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FarmerDashboardUiState(
    val dashboard: FarmerDashboardResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FarmerDashboardViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(FarmerDashboardUiState())
    val uiState: StateFlow<FarmerDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.getFarmerDashboard()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _uiState.value = FarmerDashboardUiState(dashboard = body)
                } else {
                    val message = when (response.code()) {
                        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
                        403 -> "Este resumen solo está disponible para agricultores."
                        404 -> "No se encontró tu perfil de agricultor."
                        else -> "No se pudo cargar el resumen (${response.code()})."
                    }
                    _uiState.update { it.copy(errorMessage = message) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "No se pudo conectar con AgroConecta.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
