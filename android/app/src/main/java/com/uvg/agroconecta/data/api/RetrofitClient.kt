package com.uvg.agroconecta.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ─── Session / Token Storage ─────────────────────────────────────────────────

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

object SessionManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val USER_ID_KEY = intPreferencesKey("user_id")
    private val FARMER_ID_KEY = intPreferencesKey("farmer_id")

    suspend fun saveSession(context: Context, token: String, nombre: String, userId: Int, farmerId: Int = -1) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_NAME_KEY] = nombre
            prefs[USER_ID_KEY] = userId
            prefs[FARMER_ID_KEY] = farmerId
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
}

// ─── Retrofit Client ─────────────────────────────────────────────────────────

object RetrofitClient {

    // Change to your actual server IP. In emulator: 10.0.2.2 = localhost.
    // For Azure VM: 20.63.8.63
    private const val BASE_URL = "http://20.63.8.63:8080/api/"

    @Volatile
    private var currentToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        currentToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val gson = GsonBuilder().setLenient().create()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val apiService: ApiService = retrofit.create(ApiService::class.java)

    fun getService(token: String? = null): ApiService {
        currentToken = token
        return apiService
    }
}
