package com.uvg.agroconecta.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteUiState(
    val favoriteProducts: List<Product> = emptyList(),
    val favoriteIds: Set<Int> = emptySet(),
    val pendingProductIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    // Se conserva la ultima respuesta completa para poder restaurar visualmente
    // un producto si una eliminacion optimista falla y DataStore hace rollback.
    private var loadedFavorites: List<Product> = emptyList()

    init {
        observeFavoriteIds()
    }

    fun loadFavorites() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val favorites = repository.loadFavorites()
                loadedFavorites = favorites
                _uiState.update { state ->
                    state.copy(
                        favoriteProducts = favorites.filter { it.id in state.favoriteIds },
                        isLoading = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar los favoritos"
                    )
                }
            }
        }
    }

    fun toggleFavorite(productId: Int) {
        val state = _uiState.value
        if (productId <= 0 || productId in state.pendingProductIds) return

        val shouldBeFavorite = productId !in state.favoriteIds
        _uiState.update {
            it.copy(
                pendingProductIds = it.pendingProductIds + productId,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                repository.setFavorite(productId, shouldBeFavorite)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "No se pudo actualizar el favorito"
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(pendingProductIds = it.pendingProductIds - productId)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeFavoriteIds() {
        viewModelScope.launch {
            repository.favoriteIds.collectLatest { favoriteIds ->
                _uiState.update {
                    it.copy(
                        favoriteIds = favoriteIds,
                        favoriteProducts = loadedFavorites.filter { product ->
                            product.id in favoriteIds
                        }
                    )
                }
            }
        }
    }
}
