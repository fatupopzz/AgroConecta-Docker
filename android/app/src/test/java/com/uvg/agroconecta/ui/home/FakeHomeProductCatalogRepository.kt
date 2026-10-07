package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.ProductCacheState
import com.uvg.agroconecta.data.repository.ProductCatalogRepository
import com.uvg.agroconecta.data.repository.ProductLoadResult
import com.uvg.agroconecta.data.repository.ProductRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeHomeProductCatalogRepository(
    cachedProducts: List<Product> = emptyList(),
    online: Boolean = true,
    var loadResult: ProductLoadResult = ProductLoadResult.Success(emptyList(), 0),
    var currentCacheState: ProductCacheState = ProductCacheState.EMPTY
) : ProductCatalogRepository {
    private val cached = MutableStateFlow(cachedProducts)
    val onlineState = MutableStateFlow(online)
    val requests = mutableListOf<ProductRequest>()

    override val connectivity: Flow<Boolean> = onlineState

    override fun observeCachedProducts(): Flow<List<Product>> = cached

    override suspend fun loadProducts(request: ProductRequest): ProductLoadResult {
        requests += request
        return loadResult
    }

    override suspend fun cacheState(): ProductCacheState = currentCacheState
}

internal class FakeHomeLocationProvider(
    var coordinates: GeoCoordinates? = null,
    var error: Throwable? = null
) : CurrentLocationProvider {
    var requests: Int = 0

    override suspend fun getCurrentCoordinates(): GeoCoordinates? {
        requests += 1
        error?.let { throw it }
        return coordinates
    }
}
