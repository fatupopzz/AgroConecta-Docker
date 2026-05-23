package com.uvg.agroconecta.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.annotations.SerializedName
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.api.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// ─── Model ───────────────────────────────────────────────────────────────────

data class FarmerProfile(
    @SerializedName("id_agricultor")       val idAgricultor: Int,
    val nombre: String?,
    val email: String?,
    val telefono: String?,
    val departamento: String?,
    val municipio: String?,
    @SerializedName("tipo_agricultor")     val tipoAgricultor: String?,
    @SerializedName("tamano_terreno_ha")   val tamanoTerrenoHa: Double?,
    @SerializedName("cultivos_principales") val cultivosPrincipales: String?,
    @SerializedName("tiene_membresia")     val tieneMembresia: Boolean?,
    @SerializedName("fecha_registro")      val fechaRegistro: String?
)

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val profile: FarmerProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut

    fun loadProfile(context: Context) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val token    = SessionManager.getToken(context).first()
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1

                if (token.isNullOrBlank() || farmerId == -1) {
                    _uiState.value = ProfileUiState.Error("Sesión no válida. Vuelve a iniciar sesión.")
                    return@launch
                }

                val response = RetrofitClient.getService(token)
                    .getFarmerProfile("Bearer $token", farmerId)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ProfileUiState.Success(response.body()!!)
                } else {
                    _uiState.value = ProfileUiState.Error(
                        when (response.code()) {
                            403  -> "No tienes permiso para ver este perfil."
                            404  -> "Perfil no encontrado."
                            else -> "Error al cargar perfil (${response.code()})"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    fun logout(context: Context, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _isLoggingOut.value = true
            SessionManager.clearSession(context)
            _isLoggingOut.value = false
            onLoggedOut()
        }
    }
}

// ─── ApiService extension (agregar a ApiService.kt) ──────────────────────────
// Añade este método a la interface ApiService existente:
//
//    @GET("farmers/profile/{id}")
//    suspend fun getFarmerProfile(
//        @Header("Authorization") token: String,
//        @Path("id") farmerId: Int
//    ): Response<FarmerProfile>