package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.FarmLocation
import com.uvg.agroconecta.data.models.SaveFarmLocationRequest
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lo que necesitan las pantallas que solo leen la ubicación de la finca
 * (Inicio, alertas, checkout). Es una interfaz para que sus ViewModels se
 * puedan probar sin red.
 */
interface FarmLocationSource {
    /** La ubicación guardada, o null si no hay o no se pudo consultar. */
    suspend fun savedLocation(): FarmLocation?
}

/** Para pruebas y para quien no es agricultor: nunca hay ubicación guardada. */
object NoFarmLocation : FarmLocationSource {
    override suspend fun savedLocation(): FarmLocation? = null
}

/**
 * No guarda nada en memoria a propósito: el repositorio vive toda la app y,
 * si se cierra sesión y entra otro agricultor, una copia guardada le
 * mostraría la finca del anterior. Una consulta por pantalla es barata.
 */
@Singleton
class FarmLocationRepository @Inject constructor(
    private val api: ApiService
) : FarmLocationSource {

    override suspend fun savedLocation(): FarmLocation? =
        try {
            load()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Sin señal se sigue con el GPS como antes; la ubicación guardada es
            // una mejora, no un requisito para usar la pantalla.
            null
        }

    /** Como [savedLocation], pero avisa los errores para la pantalla de edición. */
    suspend fun load(): FarmLocation? {
        val response = api.getFarmLocation()
        return when {
            response.isSuccessful -> response.body()
            response.code() == 404 -> null
            else -> throw HttpException(response)
        }
    }

    suspend fun save(request: SaveFarmLocationRequest): FarmLocation {
        val response = api.saveFarmLocation(request)
        val body = response.body()
        if (!response.isSuccessful || body == null) throw HttpException(response)
        return body
    }
}
