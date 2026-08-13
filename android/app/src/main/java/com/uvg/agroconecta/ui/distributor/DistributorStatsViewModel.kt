package com.uvg.agroconecta.ui.distributor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.DistributorStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DistributorStatsUiState(
    val stats: DistributorStatsResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DistributorStatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DistributorStatsUiState())
    val uiState: StateFlow<DistributorStatsUiState> = _uiState.asStateFlow()

    fun loadStats(distributorId: Int, token: String?) {
        if (distributorId <= 0 || token.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Sesión de distribuidor inválida")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {
                val response = RetrofitClient.getService(token)
                    .getDistributorStats(distributorId)

                if (response.isSuccessful) {
                    val stats = response.body()
                    if (stats != null) {
                        _uiState.update { it.copy(stats = stats) }
                    } else {
                        _uiState.update {
                            it.copy(errorMessage = "La respuesta de estadísticas está vacía")
                        }
                    }
                } else {
                    val message = when (response.code()) {
                        401 -> "La sesión expiró. Vuelve a iniciar sesión"
                        403 -> "No tienes permiso para consultar estas estadísticas"
                        404 -> "No se encontró el perfil del distribuidor"
                        else -> "No se pudieron cargar las estadísticas (${response.code()})"
                    }
                    _uiState.update { it.copy(errorMessage = message) }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error de conexión al cargar las estadísticas")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
