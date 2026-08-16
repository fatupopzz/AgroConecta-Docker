package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.local.ProductCacheDao
import com.uvg.agroconecta.data.local.ProductCacheEntity
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.models.ProductsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ProductCatalogRepositoryTest {

    @Test
    fun `successful API response replaces Room cache`() = runTest {
        val dao = InMemoryProductCacheDao(listOf(entity(id = 1, timestamp = 1L)))
        val api = FakeProductCatalogApi(response = Response.success(response(product(id = 2))))
        val repository = repository(api = api, dao = dao, now = 50_000L)

        val result = repository.loadProducts(ProductRequest())

        assertTrue(result is ProductLoadResult.Success)
        assertEquals(listOf(2), dao.getAll().map { it.id })
        assertEquals(50_000L, dao.getAll().single().timestamp)
    }

    @Test
    fun `paginated API response updates Room without deleting prior products`() = runTest {
        val dao = InMemoryProductCacheDao(listOf(entity(id = 1, timestamp = 1L)))
        val api = FakeProductCatalogApi(response = Response.success(response(product(id = 2))))
        val repository = repository(api = api, dao = dao, now = 60_000L)

        repository.loadProducts(ProductRequest(page = 2))

        assertEquals(listOf(1, 2), dao.getAll().map { it.id })
        assertEquals(60_000L, dao.getAll().last().timestamp)
    }

    @Test
    fun `API failure keeps the existing cache`() = runTest {
        val original = entity(id = 4, timestamp = 100L)
        val dao = InMemoryProductCacheDao(listOf(original))
        val repository = repository(
            api = FakeProductCatalogApi(error = IllegalStateException("timeout")),
            dao = dao
        )

        val result = repository.loadProducts(ProductRequest())

        assertTrue(result is ProductLoadResult.Failure)
        assertEquals(listOf(original), dao.getAll())
    }

    @Test
    fun `offline state skips API and leaves cache available`() = runTest {
        val api = FakeProductCatalogApi(response = Response.success(response(product(id = 9))))
        val dao = InMemoryProductCacheDao(listOf(entity(id = 5, timestamp = 1L)))
        val repository = repository(api = api, dao = dao, online = false)

        val result = repository.loadProducts(ProductRequest())

        assertEquals(ProductLoadResult.Offline, result)
        assertEquals(0, api.calls)
        assertEquals(listOf(5), dao.getAll().map { it.id })
    }

    @Test
    fun `online state requests API`() = runTest {
        val api = FakeProductCatalogApi(response = Response.success(response(product(id = 6))))
        val repository = repository(api = api)

        repository.loadProducts(ProductRequest())

        assertEquals(1, api.calls)
    }

    @Test
    fun `cache is fresh before 24 hours`() = runTest {
        val timestamp = 1_000L
        val repository = repository(
            dao = InMemoryProductCacheDao(listOf(entity(1, timestamp))),
            now = timestamp + PRODUCT_CACHE_TTL_MILLIS - 1L
        )

        assertEquals(ProductCacheState.FRESH, repository.cacheState())
        assertFalse(isProductCacheExpired(timestamp, timestamp + PRODUCT_CACHE_TTL_MILLIS - 1L))
    }

    @Test
    fun `cache is expired after 24 hours`() = runTest {
        val timestamp = 1_000L
        val repository = repository(
            dao = InMemoryProductCacheDao(listOf(entity(1, timestamp))),
            now = timestamp + PRODUCT_CACHE_TTL_MILLIS + 1L
        )

        assertEquals(ProductCacheState.EXPIRED, repository.cacheState())
        assertTrue(isProductCacheExpired(timestamp, timestamp + PRODUCT_CACHE_TTL_MILLIS + 1L))
    }

    @Test
    fun `cache expires at the exact 24 hour boundary`() = runTest {
        val timestamp = 1_000L
        val repository = repository(
            dao = InMemoryProductCacheDao(listOf(entity(1, timestamp))),
            now = timestamp + PRODUCT_CACHE_TTL_MILLIS
        )

        assertEquals(ProductCacheState.EXPIRED, repository.cacheState())
        assertTrue(isProductCacheExpired(timestamp, timestamp + PRODUCT_CACHE_TTL_MILLIS))
    }

    private fun repository(
        api: FakeProductCatalogApi = FakeProductCatalogApi(
            response = Response.success(response())
        ),
        dao: InMemoryProductCacheDao = InMemoryProductCacheDao(),
        online: Boolean = true,
        now: Long = 10_000L
    ) = OfflineFirstProductCatalogRepository(
        api = api,
        dao = dao,
        connectivityMonitor = FakeConnectivityMonitor(online),
        timeProvider = TimeProvider { now }
    )

    private fun response(vararg products: Product) = ProductsResponse(
        page = 1,
        limit = 10,
        total = products.size,
        products = products.toList()
    )

    private fun product(id: Int) = Product(
        id = id,
        nombre = "Producto $id",
        marca = null,
        descripcion = "Descripción $id",
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 0.0,
        categoria = "Semillas",
        precioDesde = 25.0,
        numDistribuidores = 1,
        activo = true
    )

    private fun entity(id: Int, timestamp: Long) = ProductCacheEntity(
        id = id,
        nombre = "Producto $id",
        categoria = "Semillas",
        precio = 25.0,
        descripcion = "Descripción $id",
        timestamp = timestamp
    )

    private class FakeProductCatalogApi(
        private val response: Response<ProductsResponse>? = null,
        private val error: Throwable? = null
    ) : ProductCatalogApi {
        var calls: Int = 0

        override suspend fun getProducts(request: ProductRequest): Response<ProductsResponse> {
            calls += 1
            error?.let { throw it }
            return checkNotNull(response)
        }
    }

    private class FakeConnectivityMonitor(initialOnline: Boolean) : ConnectivityMonitor {
        private val online = MutableStateFlow(initialOnline)
        override val status: Flow<Boolean> = online
        override fun isOnline(): Boolean = online.value
    }

    private class InMemoryProductCacheDao(
        initial: List<ProductCacheEntity> = emptyList()
    ) : ProductCacheDao() {
        private val products = MutableStateFlow(initial.sortedBy { it.id })

        override fun observeAll(): Flow<List<ProductCacheEntity>> = products

        override suspend fun getAll(): List<ProductCacheEntity> = products.value

        override suspend fun insertAll(products: List<ProductCacheEntity>) {
            val byId = this.products.value.associateBy { it.id }.toMutableMap()
            products.forEach { byId[it.id] = it }
            this.products.value = byId.values.sortedBy { it.id }
        }

        override suspend fun deleteAll() {
            products.value = emptyList()
        }

        override suspend fun oldestTimestamp(): Long? = products.value.minOfOrNull { it.timestamp }
    }
}
