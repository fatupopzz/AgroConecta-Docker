package com.uvg.agroconecta.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

object SessionManager {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val USER_ID_KEY = intPreferencesKey("user_id")
    private val FARMER_ID_KEY = intPreferencesKey("farmer_id")
    private val TIPO_USUARIO_KEY = stringPreferencesKey("tipo_usuario")
    private val PERFIL_ID_KEY = intPreferencesKey("perfil_id")
    private val DELIVERY_ADDRESS_KEY = stringPreferencesKey("delivery_address")

    suspend fun saveSession(
        context: Context,
        token: String,
        nombre: String,
        userId: Int,
        farmerId: Int = -1,
        tipoUsuario: String = "",
        perfilId: Int = -1
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY]        = token
            prefs[USER_NAME_KEY]    = nombre
            prefs[USER_ID_KEY]      = userId
            prefs[FARMER_ID_KEY]    = farmerId
            prefs[TIPO_USUARIO_KEY] = tipoUsuario
            prefs[PERFIL_ID_KEY]    = perfilId
        }
    }

    suspend fun clearSession(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    suspend fun expireSession(context: Context) {
        clearSession(context)
        _sessionExpired.emit(Unit)
    }

    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[TOKEN_KEY] }

    fun getUserName(context: Context): Flow<String?> =
        context.dataStore.data.map { it[USER_NAME_KEY] }

    fun getUserId(context: Context): Flow<Int?> =
        context.dataStore.data.map { it[USER_ID_KEY] }

    fun getFarmerId(context: Context): Flow<Int?> =
        context.dataStore.data.map { it[FARMER_ID_KEY] }

    fun getTipoUsuario(context: Context): Flow<String?> =
        context.dataStore.data.map { it[TIPO_USUARIO_KEY] }

    fun getPerfilId(context: Context): Flow<Int?> =
        context.dataStore.data.map { it[PERFIL_ID_KEY] }

    suspend fun saveDeliveryAddress(context: Context, address: String) {
        context.dataStore.edit { prefs ->
            prefs[DELIVERY_ADDRESS_KEY] = address
        }
    }

    fun getDeliveryAddress(context: Context): Flow<String?> =
        context.dataStore.data.map { it[DELIVERY_ADDRESS_KEY] }
}
