package com.uvg.agroconecta.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private var activeUserId: Int? = null
    private var sessionGeneration: Long = 0
    private var sessionJob: Job = newSessionJob()
    private var sessionScope = CoroutineScope(viewModelScope.coroutineContext + sessionJob)

    fun onUserChanged(userId: Int?) {
        val normalizedUserId = userId?.takeIf { it > 0 }
        if (activeUserId == normalizedUserId) return

        sessionJob.cancel()
        sessionGeneration += 1
        sessionJob = newSessionJob()
        sessionScope = CoroutineScope(viewModelScope.coroutineContext + sessionJob)
        activeUserId = normalizedUserId
        loadedFavorites = emptyList()
        _uiState.value = FavoriteUiState()

        if (normalizedUserId != null) {
            observeFavoriteIds(normalizedUserId, sessionGeneration)
            loadFavorites()
        }
    }

    fun loadFavorites() {
        val userId = activeUserId ?: return
        if (_uiState.value.isLoading) return
        val generation = sessionGeneration

        sessionScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val favorites = repository.loadFavorites(userId)
                if (!isCurrentSession(userId, generation)) return@launch

                loadedFavorites = favorites
                val favoriteIds = favorites.mapTo(mutableSetOf(), Product::id)
                _uiState.update {
                    it.copy(
                        favoriteProducts = favorites,
                        favoriteIds = favoriteIds,
                        isLoading = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (isCurrentSession(userId, generation)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar los favoritos"
                        )
                    }
                }
            }
        }
    }

    fun toggleFavorite(productId: Int) {
        val userId = activeUserId ?: return
        val state = _uiState.value
        if (productId <= 0 || productId in state.pendingProductIds) return
        val generation = sessionGeneration
        val shouldBeFavorite = productId !in state.favoriteIds
        _uiState.update {
            it.copy(
                pendingProductIds = it.pendingProductIds + productId,
                errorMessage = null
            )
        }

        sessionScope.launch {
            try {
                repository.setFavorite(userId, productId, shouldBeFavorite)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (isCurrentSession(userId, generation)) {
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "No se pudo actualizar el favorito"
                        )
                    }
                }
            } finally {
                if (isCurrentSession(userId, generation)) {
                    _uiState.update {
                        it.copy(pendingProductIds = it.pendingProductIds - productId)
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeFavoriteIds(userId: Int, generation: Long) {
        sessionScope.launch {
            repository.favoriteIds(userId).collectLatest { favoriteIds ->
                if (isCurrentSession(userId, generation)) {
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

    private fun isCurrentSession(userId: Int, generation: Long): Boolean =
        activeUserId == userId && sessionGeneration == generation

    private fun newSessionJob(): Job =
        SupervisorJob(viewModelScope.coroutineContext[Job])
}
