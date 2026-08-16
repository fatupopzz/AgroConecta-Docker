package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.local.ProductCacheDao
import com.uvg.agroconecta.data.local.ProductCacheEntity
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.models.ProductsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

const val PRODUCT_CACHE_TTL_MILLIS: Long = 24L * 60L * 60L * 1_000L

data class ProductRequest(
    val page: Int = 1,
    val limit: Int = 10,
    val name: String? = null,
    val categoryId: Int? = null
) {
    val replacesCache: Boolean
        get() = page == 1 && name.isNullOrBlank() && categoryId == null
}

sealed interface ProductLoadResult {
    data class Success(
        val products: List<Product>,
        val total: Int
    ) : ProductLoadResult

    data object Offline : ProductLoadResult

    data class Failure(val message: String) : ProductLoadResult
}

enum class ProductCacheState {
    EMPTY,
    FRESH,
    EXPIRED
}

fun interface TimeProvider {
    fun nowMillis(): Long
}

interface ProductCatalogRepository {
    val connectivity: Flow<Boolean>
    fun observeCachedProducts(): Flow<List<Product>>
    suspend fun loadProducts(request: ProductRequest): ProductLoadResult
    suspend fun cacheState(): ProductCacheState
}

internal interface ProductCatalogApi {
    suspend fun getProducts(request: ProductRequest): Response<ProductsResponse>
}

internal class RetrofitProductCatalogApi(
    private val apiService: ApiService
) : ProductCatalogApi {
    override suspend fun getProducts(request: ProductRequest): Response<ProductsResponse> =
        apiService.getProducts(
            page = request.page,
            limit = request.limit,
            nombre = request.name,
            idCategoria = request.categoryId
        )
}

class OfflineFirstProductCatalogRepository internal constructor(
    private val api: ProductCatalogApi,
    private val dao: ProductCacheDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val timeProvider: TimeProvider
) : ProductCatalogRepository {

    private val requestMutex = Mutex()

    override val connectivity: Flow<Boolean> = connectivityMonitor.status

    override fun observeCachedProducts(): Flow<List<Product>> =
        dao.observeAll().map { entities -> entities.map(ProductCacheEntity::toProduct) }

    override suspend fun loadProducts(request: ProductRequest): ProductLoadResult =
        requestMutex.withLock {
            if (!connectivityMonitor.isOnline()) {
                return@withLock ProductLoadResult.Offline
            }

            val response = try {
                api.getProducts(request)
            } catch (_: Exception) {
                return@withLock ProductLoadResult.Failure(
                    "No se pudo actualizar el catálogo. Se muestran los productos guardados."
                )
            }

            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return@withLock ProductLoadResult.Failure(
                    "No se pudo actualizar el catálogo (${response.code()})."
                )
            }

            val timestamp = timeProvider.nowMillis()
            val cachedProducts = body.products.map { product ->
                product.toCacheEntity(timestamp)
            }
            if (request.replacesCache) {
                dao.replaceAll(cachedProducts)
            } else if (cachedProducts.isNotEmpty()) {
                dao.insertAll(cachedProducts)
            }

            ProductLoadResult.Success(
                products = body.products,
                total = body.total
            )
        }

    override suspend fun cacheState(): ProductCacheState {
        val timestamp = dao.oldestTimestamp() ?: return ProductCacheState.EMPTY
        return if (isProductCacheExpired(timestamp, timeProvider.nowMillis())) {
            ProductCacheState.EXPIRED
        } else {
            ProductCacheState.FRESH
        }
    }
}

internal fun Product.toCacheEntity(timestamp: Long): ProductCacheEntity = ProductCacheEntity(
    id = id,
    nombre = nombre,
    categoria = categoria,
    precio = precioDesde,
    descripcion = descripcion,
    timestamp = timestamp
)

internal fun ProductCacheEntity.toProduct(): Product = Product(
    id = id,
    nombre = nombre,
    marca = null,
    descripcion = descripcion,
    composicion = null,
    dosis = null,
    instrucciones = null,
    calificacion = 0.0,
    categoria = categoria,
    precioDesde = precio,
    numDistribuidores = null,
    activo = true
)

// El límite exacto (edad >= 24 h) ya está vencido. La fila no se elimina:
// continúa disponible como último recurso si no existe conexión validada.
internal fun isProductCacheExpired(timestamp: Long, nowMillis: Long): Boolean =
    nowMillis - timestamp >= PRODUCT_CACHE_TTL_MILLIS
