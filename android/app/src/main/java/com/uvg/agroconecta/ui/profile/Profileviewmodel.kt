package com.uvg.agroconecta.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.annotations.SerializedName
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.MeResponse
import com.uvg.agroconecta.data.models.UpdateMyProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FarmerProfile(
    @SerializedName("id_agricultor") val idAgricultor: Int,
    val nombre: String?,
    val apellido: String? = null,
    val email: String?,
    val telefono: String?,
    val departamento: String?,
    val municipio: String?,
    @SerializedName("tipo_agricultor") val tipoAgricultor: String?,
    @SerializedName("tamano_terreno_ha") val tamanoTerrenoHa: Double?,
    @SerializedName("cultivos_principales") val cultivosPrincipales: String?,
    @SerializedName("tiene_membresia") val tieneMembresia: Boolean?,
    @SerializedName("fecha_registro") val fechaRegistro: String?
)

data class DistributorProfile(
    @SerializedName("id_distribuidor") val idDistribuidor: Int,
    @SerializedName("nombre_negocio") val nombreNegocio: String,
    val nombre: String?,
    val apellido: String? = null,
    val email: String?,
    val telefono: String?,
    val departamento: String?,
    val direccion: String?,
    val nit: String?,
    @SerializedName("estado_verificacion") val estadoVerificacion: String?,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double?
)

sealed class ProfileData {
    data class Farmer(val profile: FarmerProfile) : ProfileData()
    data class Distributor(val profile: DistributorProfile) : ProfileData()
}

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val data: ProfileData) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

data class ProfileEditDraft(
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val email: String = "",
    val departamento: String = "",
    val municipio: String = "",
    val nombreNegocio: String = "",
    val nit: String = "",
    val direccion: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val response = api.getMe()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ProfileUiState.Success(response.body()!!.toProfileData())
                } else {
                    _uiState.value = ProfileUiState.Error("Error al cargar perfil (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    fun saveProfile(draft: ProfileEditDraft, onSaved: () -> Unit) {
        if (draft.nombre.isBlank() || draft.telefono.isBlank()) {
            _saveError.value = "Nombre y teléfono son obligatorios."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            try {
                val response = api.updateMe(
                    UpdateMyProfileRequest(
                        nombre = draft.nombre.trim(),
                        apellido = draft.apellido.trim().ifBlank { null },
                        telefono = draft.telefono.trim(),
                        email = draft.email.trim().ifBlank { null },
                        departamento = draft.departamento.trim().ifBlank { null },
                        municipio = draft.municipio.trim().ifBlank { null },
                        nombreNegocio = draft.nombreNegocio.trim().ifBlank { null },
                        nit = draft.nit.trim().ifBlank { null },
                        direccion = draft.direccion.trim().ifBlank { null }
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ProfileUiState.Success(response.body()!!.toProfileData())
                    onSaved()
                } else {
                    _saveError.value = when (response.code()) {
                        409 -> "El teléfono, correo o NIT ya está registrado."
                        400 -> "Revisa los datos ingresados."
                        else -> "No se pudo guardar el perfil (${response.code()})."
                    }
                }
            } catch (e: Exception) {
                _saveError.value = "Error de conexión: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    fun logout(context: Context, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            SessionManager.clearSession(context)
            onLoggedOut()
        }
    }

    private fun MeResponse.toProfileData(): ProfileData {
        val profile = perfil ?: error("El usuario no tiene un perfil asociado")
        return when (user.tipoUsuario) {
            "agricultor" -> ProfileData.Farmer(
                FarmerProfile(
                    idAgricultor = profile.idAgricultor ?: error("Perfil de agricultor inválido"),
                    nombre = user.nombre,
                    apellido = user.apellido,
                    email = user.email,
                    telefono = user.telefono,
                    departamento = profile.departamento,
                    municipio = profile.municipio,
                    tipoAgricultor = profile.tipoAgricultor,
                    tamanoTerrenoHa = profile.tamanoTerrenoHa,
                    cultivosPrincipales = profile.cultivosPrincipales,
                    tieneMembresia = profile.tieneMembresia,
                    fechaRegistro = null
                )
            )
            "distribuidor" -> ProfileData.Distributor(
                DistributorProfile(
                    idDistribuidor = profile.idDistribuidor ?: error("Perfil de distribuidor inválido"),
                    nombreNegocio = profile.nombreNegocio ?: "Distribuidor",
                    nombre = user.nombre,
                    apellido = user.apellido,
                    email = user.email,
                    telefono = user.telefono,
                    departamento = profile.departamento,
                    direccion = profile.direccion,
                    nit = profile.nit,
                    estadoVerificacion = profile.estadoVerificacion,
                    calificacionPromedio = profile.calificacionPromedio
                )
            )
            else -> error("Tipo de usuario desconocido")
        }
    }
}
