package com.uvg.agroconecta.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.annotations.SerializedName
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.api.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Models ───────────────────────────────────────────────────────────────────

data class FarmerProfile(
    @SerializedName("id_agricultor")        val idAgricultor: Int,
    val nombre: String?,
    val email: String?,
    val telefono: String?,
    val departamento: String?,
    val municipio: String?,
    @SerializedName("tipo_agricultor")      val tipoAgricultor: String?,
    @SerializedName("tamano_terreno_ha")    val tamanoTerrenoHa: Double?,
    @SerializedName("cultivos_principales") val cultivosPrincipales: String?,
    @SerializedName("tiene_membresia")      val tieneMembresia: Boolean?,
    @SerializedName("fecha_registro")       val fechaRegistro: String?
)

data class DistributorProfile(
    @SerializedName("id_distribuidor")       val idDistribuidor: Int,
    @SerializedName("nombre_negocio")        val nombreNegocio: String,
    val nombre: String?,
    val email: String?,
    val telefono: String?,
    val departamento: String?,
    val direccion: String?,
    val nit: String?,
    @SerializedName("estado_verificacion")   val estadoVerificacion: String?,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double?
)

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class ProfileData {
    data class Farmer(val profile: FarmerProfile) : ProfileData()
    data class Distributor(val profile: DistributorProfile) : ProfileData()
}

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val data: ProfileData) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut

    fun loadProfile(context: Context) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val token       = SessionManager.getToken(context).first()
                val tipoUsuario = SessionManager.getTipoUsuario(context).first()
                val perfilId    = SessionManager.getPerfilId(context).first() ?: -1

                if (token.isNullOrBlank() || perfilId == -1) {
                    _uiState.value = ProfileUiState.Error("Sesión no válida. Vuelve a iniciar sesión.")
                    return@launch
                }

                // Aca arriba ya se descarto el token en blanco, asi que las dos
                // ramas pueden mandar la cabecera armada sin volver a chequear.
                val bearer = "Bearer $token"

                when (tipoUsuario) {
                    "agricultor" -> {
                        val response = api.getFarmerProfile(bearer, perfilId)
                        if (response.isSuccessful && response.body() != null) {
                            _uiState.value = ProfileUiState.Success(ProfileData.Farmer(response.body()!!))
                        } else {
                            _uiState.value = ProfileUiState.Error("Error al cargar perfil (${response.code()})")
                        }
                    }
                    "distribuidor" -> {
                        val response = api.getDistributorById(perfilId, bearer)
                        if (response.isSuccessful && response.body() != null) {
                            _uiState.value = ProfileUiState.Success(ProfileData.Distributor(response.body()!!))
                        } else {
                            _uiState.value = ProfileUiState.Error("Error al cargar perfil (${response.code()})")
                        }
                    }
                    else -> {
                        _uiState.value = ProfileUiState.Error("Tipo de usuario desconocido.")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    fun logout(context: Context, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            SessionManager.clearSession(context)
            onLoggedOut()
        }
    }
}