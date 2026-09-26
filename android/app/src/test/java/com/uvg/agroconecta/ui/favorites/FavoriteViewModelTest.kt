package com.uvg.agroconecta.ui.favorites

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.FavoriteRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `exposes locally persisted favorite IDs without loading the backend`() {
        val repository = FakeFavoriteRepository(initialIds = setOf(3, 7))

        val viewModel = FavoriteViewModel(repository)

        assertEquals(setOf(3, 7), viewModel.uiState.value.favoriteIds)
        assertEquals(0, repository.loadRequests)
    }

    @Test
    fun `loadFavorites exposes backend products and refreshes local IDs`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val favorites = listOf(product(4), product(8))
            val repository = FakeFavoriteRepository(
                initialIds = setOf(99),
                favorites = favorites
            )
            val viewModel = FavoriteViewModel(repository)

            viewModel.loadFavorites()
            advanceUntilIdle()

            assertEquals(1, repository.loadRequests)
            assertEquals(setOf(4, 8), viewModel.uiState.value.favoriteIds)
            assertEquals(favorites, viewModel.uiState.value.favoriteProducts)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `toggle exposes optimistic state while backend request is pending`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val responseGate = CompletableDeferred<Unit>()
            val repository = FakeFavoriteRepository(mutationGate = responseGate)
            val viewModel = FavoriteViewModel(repository)

            viewModel.toggleFavorite(7)
            runCurrent()

            assertEquals(setOf(7), viewModel.uiState.value.favoriteIds)
            assertEquals(setOf(7), viewModel.uiState.value.pendingProductIds)
            assertEquals(listOf(7 to true), repository.mutations)

            responseGate.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.pendingProductIds.isEmpty())
            assertEquals(setOf(7), viewModel.uiState.value.favoriteIds)
        }

    @Test
    fun `failed removal restores product and exposes repository error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val favorite = product(7)
            val repository = FakeFavoriteRepository(favorites = listOf(favorite))
            val viewModel = FavoriteViewModel(repository)
            viewModel.loadFavorites()
            advanceUntilIdle()
            repository.mutationError = IllegalStateException(
                "No se pudo eliminar el favorito (503)"
            )

            viewModel.toggleFavorite(7)
            advanceUntilIdle()

            assertEquals(setOf(7), viewModel.uiState.value.favoriteIds)
            assertEquals(listOf(favorite), viewModel.uiState.value.favoriteProducts)
            assertEquals(
                "No se pudo eliminar el favorito (503)",
                viewModel.uiState.value.errorMessage
            )
            assertTrue(viewModel.uiState.value.pendingProductIds.isEmpty())
        }

    @Test
    fun `ignores repeated toggle while the same product is pending`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val responseGate = CompletableDeferred<Unit>()
            val repository = FakeFavoriteRepository(mutationGate = responseGate)
            val viewModel = FavoriteViewModel(repository)

            viewModel.toggleFavorite(12)
            viewModel.toggleFavorite(12)
            runCurrent()

            assertEquals(listOf(12 to true), repository.mutations)

            responseGate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `failed load keeps local IDs and reports the error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavoriteRepository(initialIds = setOf(5)).apply {
                loadError = IllegalStateException("No se pudieron cargar los favoritos (500)")
            }
            val viewModel = FavoriteViewModel(repository)

            viewModel.loadFavorites()
            advanceUntilIdle()

            assertEquals(setOf(5), viewModel.uiState.value.favoriteIds)
            assertTrue(viewModel.uiState.value.favoriteProducts.isEmpty())
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                "No se pudieron cargar los favoritos (500)",
                viewModel.uiState.value.errorMessage
            )

            viewModel.clearError()
            assertNull(viewModel.uiState.value.errorMessage)
        }

    private fun product(id: Int) = Product(
        id = id,
        nombre = "Producto $id",
        marca = null,
        descripcion = "Descripcion $id",
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 4.5,
        categoria = "Semillas",
        precioDesde = 25.0,
        numDistribuidores = 2,
        activo = true
    )
}

private class FakeFavoriteRepository(
    initialIds: Set<Int> = emptySet(),
    private val favorites: List<Product> = emptyList(),
    private val mutationGate: CompletableDeferred<Unit>? = null
) : FavoriteRepository {
    private val ids = MutableStateFlow(initialIds)

    override val favoriteIds: Flow<Set<Int>> = ids

    var loadRequests = 0
        private set
    val mutations = mutableListOf<Pair<Int, Boolean>>()
    var loadError: Throwable? = null
    var mutationError: Throwable? = null

    override suspend fun loadFavorites(): List<Product> {
        loadRequests += 1
        loadError?.let { throw it }
        ids.value = favorites.mapTo(mutableSetOf(), Product::id)
        return favorites
    }

    override suspend fun setFavorite(productId: Int, favorite: Boolean) {
        mutations += productId to favorite
        val previousIds = ids.value
        ids.value = if (favorite) previousIds + productId else previousIds - productId

        try {
            mutationGate?.await()
            mutationError?.let { throw it }
        } catch (error: Throwable) {
            ids.value = previousIds
            throw error
        }
    }
}
