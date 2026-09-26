package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.local.FavoriteLocalDataSource
import com.uvg.agroconecta.data.models.AddFavoriteRequest
import com.uvg.agroconecta.data.models.FavoriteMutationResponse
import com.uvg.agroconecta.data.models.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Response

interface FavoriteRepository {
    fun favoriteIds(userId: Int): Flow<Set<Int>>

    suspend fun loadFavorites(userId: Int): List<Product>

    suspend fun setFavorite(userId: Int, productId: Int, favorite: Boolean)
}

internal interface FavoriteApi {
    suspend fun getFavorites(): Response<List<Product>>

    suspend fun addFavorite(request: AddFavoriteRequest): Response<FavoriteMutationResponse>

    suspend fun removeFavorite(productId: Int): Response<Unit>
}

internal class RetrofitFavoriteApi(
    private val service: ApiService
) : FavoriteApi {
    override suspend fun getFavorites(): Response<List<Product>> = service.getFavorites()

    override suspend fun addFavorite(
        request: AddFavoriteRequest
    ): Response<FavoriteMutationResponse> = service.addFavorite(request)

    override suspend fun removeFavorite(productId: Int): Response<Unit> =
        service.removeFavorite(productId)
}

class RemoteFavoriteRepository internal constructor(
    private val api: FavoriteApi,
    private val localDataSource: FavoriteLocalDataSource
) : FavoriteRepository {

    constructor(
        service: ApiService,
        localDataSource: FavoriteLocalDataSource
    ) : this(RetrofitFavoriteApi(service), localDataSource)

    override fun favoriteIds(userId: Int): Flow<Set<Int>> =
        localDataSource.favoriteIds(userId)

    override suspend fun loadFavorites(userId: Int): List<Product> {
        require(userId > 0) { "userId debe ser positivo" }
        val response = api.getFavorites()
        if (!response.isSuccessful) {
            error("No se pudieron cargar los favoritos (${response.code()})")
        }

        val favorites = response.body()
            ?: error("No se pudieron cargar los favoritos: respuesta vacía")
        localDataSource.replaceFavoriteIds(
            userId = userId,
            productIds = favorites.mapTo(mutableSetOf(), Product::id)
        )
        return favorites
    }

    override suspend fun setFavorite(userId: Int, productId: Int, favorite: Boolean) {
        require(userId > 0) { "userId debe ser positivo" }
        require(productId > 0) { "productId debe ser positivo" }

        val wasFavorite = productId in favoriteIds(userId).first()
        localDataSource.setFavorite(userId, productId, favorite)

        try {
            val response = if (favorite) {
                api.addFavorite(AddFavoriteRequest(productId))
            } else {
                api.removeFavorite(productId)
            }

            if (!response.isSuccessful) {
                error(
                    if (favorite) {
                        "No se pudo guardar el favorito (${response.code()})"
                    } else {
                        "No se pudo eliminar el favorito (${response.code()})"
                    }
                )
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                localDataSource.setFavorite(userId, productId, wasFavorite)
            }
            throw cancelled
        } catch (error: Throwable) {
            localDataSource.setFavorite(userId, productId, wasFavorite)
            throw error
        }
    }
}
