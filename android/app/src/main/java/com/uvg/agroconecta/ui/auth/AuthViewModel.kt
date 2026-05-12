package com.uvg.agroconecta.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.LoginRequest
import com.uvg.agroconecta.data.models.RegisterRequest
import com.uvg.agroconecta.data.models.TipoCuenta
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

data class RegisterDraft(
    val tipoCuenta: TipoCuenta = TipoCuenta.AGRICULTOR,
    val nombre: String = "",
    val apellido: String = "",
    val telefono: String = "",
    val email: String = "",
    val password: String = "",
    val departamento: String? = null,
    val municipio: String? = null,
    val nombreNegocio: String? = null,
    val nit: String? = null
)

class AuthViewModel : ViewModel() {

    private val _loginState = MutableLiveData<AuthState>(AuthState.Idle)
    val loginState: LiveData<AuthState> = _loginState

    private val _registerState = MutableLiveData<AuthState>(AuthState.Idle)
    val registerState: LiveData<AuthState> = _registerState

    private val _registerDraft = MutableLiveData(RegisterDraft())
    val registerDraft: LiveData<RegisterDraft> = _registerDraft

    fun updateDraft(transform: (RegisterDraft) -> RegisterDraft) {
        _registerDraft.value = transform(_registerDraft.value ?: RegisterDraft())
    }

    fun resetRegister() {
        _registerDraft.value = RegisterDraft()
        _registerState.value = AuthState.Idle
    }

    fun login(email: String, password: String, context: Context) {
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService()
                    .login(LoginRequest(email, password))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SessionManager.saveSession(context, body.token, email, -1, -1)
                    _loginState.value = AuthState.Success
                } else {
                    val msg = when (response.code()) {
                        400 -> "Usuario o contraseña incorrectos"
                        401 -> "Contraseña incorrecta"
                        404 -> "Usuario no encontrado"
                        else -> "Error al iniciar sesión (${response.code()})"
                    }
                    _loginState.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    fun submitRegister(context: Context) {
        val draft = _registerDraft.value ?: return
        _registerState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val request = RegisterRequest(
                    nombre = draft.nombre.trim(),
                    apellido = draft.apellido.trim().ifBlank { null },
                    telefono = draft.telefono.trim(),
                    email = draft.email.trim(),
                    password = draft.password,
                    tipoUsuario = draft.tipoCuenta.apiValue,
                    departamento = draft.departamento,
                    municipio = draft.municipio,
                    nombreNegocio = draft.nombreNegocio?.trim()?.ifBlank { null },
                    nit = draft.nit?.trim()?.ifBlank { null }
                )

                val response = RetrofitClient.getService().register(request)

                if (response.isSuccessful) {
                    _registerState.value = AuthState.Success
                } else {
                    val msg = when (response.code()) {
                        400 -> "Datos inválidos o usuario ya existe"
                        else -> "Error al registrarse (${response.code()})"
                    }
                    _registerState.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }
}