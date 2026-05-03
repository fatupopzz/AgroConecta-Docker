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
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _loginState = MutableLiveData<AuthState>(AuthState.Idle)
    val loginState: LiveData<AuthState> = _loginState

    private val _registerState = MutableLiveData<AuthState>(AuthState.Idle)
    val registerState: LiveData<AuthState> = _registerState

    fun login(email: String, password: String, context: Context) {
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService()
                    .login(LoginRequest(email, password))

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    // Save token — farmerId will be set after profile load
                    // For now we save -1 and will update when needed
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

    fun register(nombre: String, telefono: String, email: String, password: String, context: Context) {
        _registerState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService()
                    .register(RegisterRequest(nombre, telefono, email, password))

                if (response.isSuccessful) {
                    _registerState.value = AuthState.Success
                } else {
                    _registerState.value = AuthState.Error("Error al registrarse (${response.code()})")
                }
            } catch (e: Exception) {
                _registerState.value = AuthState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }
}
