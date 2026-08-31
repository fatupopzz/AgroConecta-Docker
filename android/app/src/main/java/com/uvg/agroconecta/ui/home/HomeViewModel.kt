package com.uvg.agroconecta.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.api.toAuthHeader
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.CropCycleRepository
import com.uvg.agroconecta.data.repository.ProductCacheState
import com.uvg.agroconecta.data.repository.ProductCatalogRepository
import com.uvg.agroconecta.data.repository.ProductLoadResult
import com.uvg.agroconecta.data.repository.ProductRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val nombreAgricultor: String = "",
    val categorias: List<Category> = emptyList(),
    val categoriaSeleccionadaId: Int? = null,
    val productosRecomendados: List<Product> = emptyList(),
    val productos: List<Product> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val searchQuery: String = "",
    val filtroPrecioMin: Int? = null,
    val filtroPrecioMax: Int? = null,
    val filtroMarca: String = "",
    val filtrosAbiertos: Boolean = false,
    val ofertaDelDia: Product? = null,
    val distribuidores: List<Distributor> = emptyList(),
    val cicloRelevante: CropCycleResponse? = null,
    val isLoadingProductos: Boolean = false,
    val isLoadingRecomendados: Boolean = false,
    val isLoadingCategorias: Boolean = false,
    val isLoadingDistribuidores: Boolean = false,
    val isLoadingCiclo: Boolean = false,
    val isOffline: Boolean = false,
    val productCacheState: ProductCacheState = ProductCacheState.EMPTY,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: ApiService,
    private val cropCycleRepository: CropCycleRepository,
    private val productCatalogRepository: ProductCatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var token: String? = null
    private var initialized = false
    private var productLoadJob: Job? = null
    private var lastConnectivity: Boolean? = null
    private var cachedProducts: List<Product> = emptyList()

    init {
        observeCachedProducts()
        observeConnectivity()
    }

    fun init(token: String?, tipoUsuario: String = "agricultor") {
        if (initialized) return
        initialized = true
        this.token = token
        if (tipoUsuario == "agricultor" && !token.isNullOrBlank()) {
            loadRecommendedProducts()
        }
        loadCategorias()
        loadProductos(reset = true)
        loadDistribuidores()
        if (tipoUsuario == "agricultor" && !token.isNullOrBlank()) {
            loadRelevantCropCycle(token)
        } else {
            _uiState.update { it.copy(cicloRelevante = null, isLoadingCiclo = false) }
        }
    }

    internal fun loadRelevantCropCycle(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCiclo = true) }
            val cycle = runCatching {
                cropCycleRepository.getRelevantCycle(token)
            }.getOrNull()
            _uiState.update {
                it.copy(cicloRelevante = cycle, isLoadingCiclo = false)
            }
        }
    }

    fun loadRecommendedProducts() {
        val currentToken = token ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecomendados = true) }
            try {
                val response = api.getRecommendedProducts(currentToken.toAuthHeader())
                _uiState.update {
                    it.copy(
                        productosRecomendados = if (response.isSuccessful) {
                            response.body().orEmpty().take(10)
                        } else {
                            emptyList()
                        },
                        isLoadingRecomendados = false
                    )
                }
            } catch (_: Exception) {
                // El catálogo general sigue disponible si las recomendaciones fallan.
                _uiState.update { it.copy(isLoadingRecomendados = false) }
            }
        }
    }

    fun setNombreAgricultor(nombre: String) {
        _uiState.update { it.copy(nombreAgricultor = nombre) }
    }

    fun loadCategorias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategorias = true) }
            try {
                val response = api.getCategories()
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            categorias = response.body() ?: emptyList(),
                            isLoadingCategorias = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingCategorias = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCategorias = false, errorMessage = e.message) }
            }
        }
    }

    fun onCategoriaSelect(categoriaId: Int?) {
        _uiState.update { it.copy(categoriaSeleccionadaId = categoriaId) }
        loadProductos(reset = true)
    }

    fun loadProductos(reset: Boolean = false) {
        val state = _uiState.value
        if (!reset && productLoadJob?.isActive == true) return
        if (!reset && !state.hasMore) return

        val page = if (reset) 1 else state.currentPage
        val request = ProductRequest(
            page = page,
            limit = 10,
            name = state.searchQuery.ifBlank { null },
            categoryId = state.categoriaSeleccionadaId
        )

        if (reset) {
            productLoadJob?.cancel()
        }
        _uiState.update { it.copy(isLoadingProductos = true, errorMessage = null) }

        productLoadJob = viewModelScope.launch {
            when (val result = productCatalogRepository.loadProducts(request)) {
                is ProductLoadResult.Success -> {
                    _uiState.update {
                        val products = if (reset) {
                            result.products
                        } else {
                            (it.productos + result.products).distinctBy(Product::id)
                        }
                        it.copy(
                            productos = products,
                            currentPage = page + 1,
                            hasMore = products.size < result.total,
                            isLoadingProductos = false,
                            ofertaDelDia = if (reset) result.products.firstOrNull() else it.ofertaDelDia
                        )
                    }
                }

                ProductLoadResult.Offline -> {
                    _uiState.update {
                        val fallback = if (reset) filterCachedProducts(it) else it.productos
                        it.copy(
                            productos = fallback,
                            ofertaDelDia = if (reset) fallback.firstOrNull() else it.ofertaDelDia,
                            hasMore = false,
                            isLoadingProductos = false
                        )
                    }
                }

                is ProductLoadResult.Failure -> {
                    _uiState.update {
                        val fallback = if (reset && it.productos.isEmpty()) {
                            filterCachedProducts(it)
                        } else {
                            it.productos
                        }
                        it.copy(
                            productos = fallback,
                            ofertaDelDia = it.ofertaDelDia ?: fallback.firstOrNull(),
                            isLoadingProductos = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
            refreshCacheState()
        }
    }

    fun loadDistribuidores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDistribuidores = true) }
            try {
                val response = api.getVerifiedDistributors(token.toAuthHeader())
                if (response.isSuccessful) {
                    val verificados = (response.body() ?: emptyList())
                        .filter { it.estadoVerificacion == "verificado" }
                    _uiState.update {
                        it.copy(
                            distribuidores = verificados,
                            isLoadingDistribuidores = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingDistribuidores = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingDistribuidores = false, errorMessage = e.message) }
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchSubmit() { loadProductos(reset = true) }

    fun applyCatalogFilter(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = normalizeCatalogQuery(query),
                categoriaSeleccionadaId = null,
                currentPage = 1,
                hasMore = true
            )
        }
        loadProductos(reset = true)
    }

    fun abrirFiltros() { _uiState.update { it.copy(filtrosAbiertos = true) } }

    fun cerrarFiltros() { _uiState.update { it.copy(filtrosAbiertos = false) } }

    fun onFiltrosAplicados(precioMin: Int?, precioMax: Int?, marca: String) {
        _uiState.update {
            it.copy(
                filtroPrecioMin = precioMin,
                filtroPrecioMax = precioMax,
                filtroMarca = marca,
                filtrosAbiertos = false
            )
        }
        loadProductos(reset = true)
    }

    fun onFiltrosLimpiados() {
        _uiState.update {
            it.copy(
                filtroPrecioMin = null,
                filtroPrecioMax = null,
                filtroMarca = "",
                filtrosAbiertos = false
            )
        }
        loadProductos(reset = true)
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    private fun observeCachedProducts() {
        viewModelScope.launch {
            productCatalogRepository.observeCachedProducts().collectLatest { products ->
                cachedProducts = products
                _uiState.update { state ->
                    val isDefaultCatalog = state.searchQuery.isBlank() &&
                        state.categoriaSeleccionadaId == null
                    if (isDefaultCatalog && (state.productos.isEmpty() || state.isOffline)) {
                        state.copy(
                            productos = products,
                            ofertaDelDia = products.firstOrNull()
                        )
                    } else {
                        state
                    }
                }
                refreshCacheState()
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            productCatalogRepository.connectivity.collectLatest { online ->
                val previous = lastConnectivity
                lastConnectivity = online
                _uiState.update { it.copy(isOffline = !online) }
                if (previous == false && online) {
                    loadProductos(reset = true)
                }
            }
        }
    }

    private fun refreshCacheState() {
        viewModelScope.launch {
            val cacheState = productCatalogRepository.cacheState()
            _uiState.update { it.copy(productCacheState = cacheState) }
        }
    }

    private fun filterCachedProducts(state: HomeUiState): List<Product> {
        val query = state.searchQuery.trim()
        val categoryName = state.categoriaSeleccionadaId?.let { selectedId ->
            state.categorias.firstOrNull { it.id == selectedId }?.nombre
        }

        return cachedProducts.filter { product ->
            val matchesQuery = query.isBlank() ||
                product.nombre.contains(query, ignoreCase = true) ||
                product.descripcion.orEmpty().contains(query, ignoreCase = true)
            val matchesCategory = categoryName == null ||
                product.categoria.equals(categoryName, ignoreCase = true)
            val matchesMinimum = state.filtroPrecioMin == null ||
                (product.precioDesde ?: Double.NEGATIVE_INFINITY) >= state.filtroPrecioMin.toDouble()
            val matchesMaximum = state.filtroPrecioMax == null ||
                (product.precioDesde ?: Double.POSITIVE_INFINITY) <= state.filtroPrecioMax.toDouble()
            matchesQuery && matchesCategory && matchesMinimum && matchesMaximum
        }
    }
}

internal fun normalizeCatalogQuery(query: String): String =
    query.trim().replace(Regex("\\s+"), " ")
