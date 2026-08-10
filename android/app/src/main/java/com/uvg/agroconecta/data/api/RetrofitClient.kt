package com.uvg.agroconecta.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit

// ─── Session / Token Storage ─────────────────────────────────────────────────

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

object SessionManager {
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

// ─── Retrofit Client ─────────────────────────────────────────────────────────

/**
 * Acceso al ApiService para los ViewModels que todavia no estan migrados a Hilt.
 *
 * Los ViewModels migrados reciben el ApiService por constructor desde
 * `di.NetworkModule`. Ambos caminos se construyen con [ApiClientFactory], asi
 * que producen clientes equivalentes.
 */
object RetrofitClient {

    private val gson = ApiClientFactory.createGson()

    private val baseClient: OkHttpClient = ApiClientFactory.createOkHttpClient()

    private val publicService: ApiService by lazy { createPublicService() }

    fun getService(token: String? = null): ApiService =
        if (token.isNullOrBlank()) {
            publicService
        } else {
            createAuthenticatedService(token)
        }

    private fun createAuthenticatedService(token: String): ApiService {
        val client = baseClient.newBuilder()
            .addInterceptor(Interceptor { chain ->
                // Solo agrega la cabecera si el endpoint no la declaro ya con
                // @Header. addHeader() acumula en vez de reemplazar, asi que sin
                // esta guarda el request salia con dos Authorization.
                val original = chain.request()
                val request = if (original.header("Authorization") == null) {
                    original.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            })
            .build()

        return createRetrofit(client).create(ApiService::class.java)
    }

    private fun createRetrofit(client: OkHttpClient): Retrofit =
        ApiClientFactory.createRetrofit(client, gson)

    private fun createPublicService(): ApiService =
        createRetrofit(baseClient).create(ApiService::class.java)
}
