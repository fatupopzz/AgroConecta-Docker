package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.local.FavoriteLocalDataSource
import com.uvg.agroconecta.data.models.AddFavoriteRequest
import com.uvg.agroconecta.data.models.FavoriteMutationResponse
import com.uvg.agroconecta.data.models.FavoriteRecord
import com.uvg.agroconecta.data.models.Product
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteRepositoryTest {

    @Test
    fun `loadFavorites refreshes local IDs from backend products`() = runTest {
        val local = FakeFavoriteLocalDataSource(setOf(99))
        val products = listOf(product(4), product(8))
        val repository = RemoteFavoriteRepository(
            api = FakeFavoriteApi(getResponse = Response.success(products)),
            localDataSource = local
        )

        val result = repository.loadFavorites()

        assertEquals(products, result)
        assertEquals(setOf(4, 8), local.currentIds())
    }

    @Test
    fun `add updates local state before backend responds`() = runTest {
        val response = CompletableDeferred<Response<FavoriteMutationResponse>>()
        val local = FakeFavoriteLocalDataSource()
        val api = FakeFavoriteApi(addCall = { response.await() })
        val repository = RemoteFavoriteRepository(api, local)

        val job = launch { repository.setFavorite(productId = 7, favorite = true) }
        runCurrent()

        assertEquals(setOf(7), local.currentIds())
        assertEquals(AddFavoriteRequest(7), api.addedRequest)
        assertFalse(job.isCompleted)

        response.complete(Response.success(mutationResponse(productId = 7)))
        job.join()
        assertTrue(job.isCompleted)
        assertEquals(setOf(7), local.currentIds())
    }

    @Test
    fun `failed add rolls back only the optimistic product`() = runTest {
        val local = FakeFavoriteLocalDataSource(setOf(3))
        val repository = RemoteFavoriteRepository(
            api = FakeFavoriteApi(
                addResponse = Response.error(500, "".toResponseBody())
            ),
            localDataSource = local
        )

        val error = runCatching {
            repository.setFavorite(productId = 7, favorite = true)
        }.exceptionOrNull()

        assertNotNull(error)
        assertEquals("No se pudo guardar el favorito (500)", error?.message)
        assertEquals(setOf(3), local.currentIds())
    }

    @Test
    fun `remove updates DataStore and calls product endpoint`() = runTest {
        val local = FakeFavoriteLocalDataSource(setOf(3, 7))
        val api = FakeFavoriteApi(removeResponse = Response.success(Unit))
        val repository = RemoteFavoriteRepository(api, local)

        repository.setFavorite(productId = 7, favorite = false)

        assertEquals(setOf(3), local.currentIds())
        assertEquals(7, api.removedProductId)
    }

    @Test
    fun `failed remove restores previous favorite state`() = runTest {
        val local = FakeFavoriteLocalDataSource(setOf(7))
        val repository = RemoteFavoriteRepository(
            api = FakeFavoriteApi(
                removeResponse = Response.error(503, "".toResponseBody())
            ),
            localDataSource = local
        )

        val error = runCatching {
            repository.setFavorite(productId = 7, favorite = false)
        }.exceptionOrNull()

        assertEquals("No se pudo eliminar el favorito (503)", error?.message)
        assertEquals(setOf(7), local.currentIds())
    }

    @Test
    fun `failed refresh keeps locally persisted IDs`() = runTest {
        val local = FakeFavoriteLocalDataSource(setOf(3, 7))
        val repository = RemoteFavoriteRepository(
            api = FakeFavoriteApi(
                getResponse = Response.error(500, "".toResponseBody())
            ),
            localDataSource = local
        )

        val error = runCatching { repository.loadFavorites() }.exceptionOrNull()

        assertEquals("No se pudieron cargar los favoritos (500)", error?.message)
        assertEquals(setOf(3, 7), local.currentIds())
    }

    private fun product(id: Int) = Product(
        id = id,
        nombre = "Producto $id",
        marca = null,
        descripcion = "Descripción $id",
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 4.5,
        categoria = "Semillas",
        precioDesde = 25.0,
        numDistribuidores = 2,
        activo = true
    )

    private fun mutationResponse(productId: Int) = FavoriteMutationResponse(
        message = "Producto guardado en favoritos",
        favorite = FavoriteRecord(
            id = 1,
            userId = 25,
            productId = productId,
            addedAt = "2026-09-25T12:00:00.000Z"
        )
    )
}

private class FakeFavoriteLocalDataSource(
    initialIds: Set<Int> = emptySet()
) : FavoriteLocalDataSource {
    private val ids = MutableStateFlow(initialIds)

    override val favoriteIds: Flow<Set<Int>> = ids

    override suspend fun setFavorite(productId: Int, favorite: Boolean) {
        ids.value = if (favorite) ids.value + productId else ids.value - productId
    }

    override suspend fun replaceFavoriteIds(productIds: Set<Int>) {
        ids.value = productIds
    }

    fun currentIds(): Set<Int> = ids.value
}

private class FakeFavoriteApi(
    private val getResponse: Response<List<Product>> = Response.success(emptyList()),
    private val addResponse: Response<FavoriteMutationResponse>? = null,
    private val removeResponse: Response<Unit> = Response.success(Unit),
    private val addCall: (suspend (AddFavoriteRequest) -> Response<FavoriteMutationResponse>)? = null
) : FavoriteApi {
    var addedRequest: AddFavoriteRequest? = null
    var removedProductId: Int? = null

    override suspend fun getFavorites(): Response<List<Product>> = getResponse

    override suspend fun addFavorite(
        request: AddFavoriteRequest
    ): Response<FavoriteMutationResponse> {
        addedRequest = request
        return addCall?.invoke(request) ?: checkNotNull(addResponse)
    }

    override suspend fun removeFavorite(productId: Int): Response<Unit> {
        removedProductId = productId
        return removeResponse
    }
}
