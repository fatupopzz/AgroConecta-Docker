package com.uvg.agroconecta.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.data.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val nombreAgricultor: String = "",

    // Categorías
    val categorias: List<Category> = emptyList(),
    val categoriaSeleccionadaId: Int? = null,

    // Productos
    val productos: List<Product> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,

    // Filtros
    val searchQuery: String = "",
    val filtroPrecioMin: Int? = null,
    val filtroPrecioMax: Int? = null,
    val filtroMarca: String = "",
    val filtrosAbiertos: Boolean = false,

    // Oferta del día
    val ofertaDelDia: Product? = null,

    // Distribuidores
    val distribuidores: List<Distributor> = emptyList(),

    // Loading / error
    val isLoadingProductos: Boolean = false,
    val isLoadingCategorias: Boolean = false,
    val isLoadingDistribuidores: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val api = RetrofitClient.getService()

    init {
        loadCategorias()
        loadProductos(reset = true)
        loadDistribuidores()
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
        if (state.isLoadingProductos) return
        if (!reset && !state.hasMore) return

        val page = if (reset) 1 else state.currentPage

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProductos = true) }
            try {
                val response = api.getProducts(
                    page = page,
                    limit = 10,
                    nombre = state.searchQuery.ifBlank { null },
                    idCategoria = state.categoriaSeleccionadaId
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val nuevos = body?.products ?: emptyList()
                    val lista = if (reset) nuevos else state.productos + nuevos
                    val total = body?.total ?: 0
                    _uiState.update {
                        it.copy(
                            productos = lista,
                            currentPage = page + 1,
                            hasMore = lista.size < total,
                            isLoadingProductos = false,
                            ofertaDelDia = if (reset) nuevos.firstOrNull() else it.ofertaDelDia
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingProductos = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingProductos = false, errorMessage = e.message) }
            }
        }
    }

    fun loadDistribuidores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDistribuidores = true) }
            try {
                val response = api.getVerifiedDistributors()
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
}