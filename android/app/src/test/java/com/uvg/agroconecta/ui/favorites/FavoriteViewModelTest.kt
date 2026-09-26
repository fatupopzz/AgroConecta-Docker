package com.uvg.agroconecta.ui.favorites

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.FavoriteRepository
import kotlinx.coroutines.CancellationException
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

    private val userId = 25

    @Test
    fun `selecting a user loads backend products and refreshes local IDs`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val favorites = listOf(product(4), product(8))
            val repository = FakeFavoriteRepository(
                initialIds = setOf(99),
                favorites = favorites
            )
            val viewModel = FavoriteViewModel(repository)

            viewModel.onUserChanged(userId)
            advanceUntilIdle()

            assertEquals(listOf(userId), repository.loadRequests)
            assertEquals(setOf(4, 8), viewModel.uiState.value.favoriteIds)
            assertEquals(favorites, viewModel.uiState.value.favoriteProducts)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `selecting the same user again does not duplicate the load`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavoriteRepository()
            val viewModel = FavoriteViewModel(repository)

            viewModel.onUserChanged(userId)
            advanceUntilIdle()
            viewModel.onUserChanged(userId)
            advanceUntilIdle()

            assertEquals(listOf(userId), repository.loadRequests)
        }

    @Test
    fun `switching users clears old IDs and cancels the previous load`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val firstUserId = 10
            val secondUserId = 20
            val firstLoadGate = CompletableDeferred<Unit>()
            val repository = FakeFavoriteRepository().apply {
                seedUser(firstUserId, setOf(7), listOf(product(7)))
                seedUser(secondUserId, setOf(12), listOf(product(12)))
                loadGates[firstUserId] = firstLoadGate
            }
            val viewModel = FavoriteViewModel(repository)

            viewModel.onUserChanged(firstUserId)
            runCurrent()
            assertEquals(setOf(7), viewModel.uiState.value.favoriteIds)
            assertTrue(viewModel.uiState.value.isLoading)

            viewModel.onUserChanged(secondUserId)
            assertFalse(7 in viewModel.uiState.value.favoriteIds)
            assertTrue(viewModel.uiState.value.favoriteProducts.none { it.id == 7 })
            advanceUntilIdle()

            assertEquals(listOf(firstUserId, secondUserId), repository.loadRequests)
            assertEquals(listOf(firstUserId), repository.cancelledLoads)
            assertEquals(setOf(12), viewModel.uiState.value.favoriteIds)
            assertEquals(listOf(product(12)), viewModel.uiState.value.favoriteProducts)
        }

    @Test
    fun `logout clears favorite state and blocks mutations without a user`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavoriteRepository(favorites = listOf(product(7)))
            val viewModel = FavoriteViewModel(repository)
            viewModel.onUserChanged(userId)
            advanceUntilIdle()
            assertEquals(setOf(7), viewModel.uiState.value.favoriteIds)

            viewModel.onUserChanged(null)
            viewModel.toggleFavorite(7)
            advanceUntilIdle()

            assertEquals(FavoriteUiState(), viewModel.uiState.value)
            assertTrue(repository.mutations.isEmpty())
        }

    @Test
    fun `toggle exposes optimistic state while backend request is pending`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val responseGate = CompletableDeferred<Unit>()
            val repository = FakeFavoriteRepository(mutationGate = responseGate)
            val viewModel = FavoriteViewModel(repository)
            viewModel.onUserChanged(userId)
            advanceUntilIdle()

            viewModel.toggleFavorite(7)
            runCurrent()

            assertEquals(setOf(7), viewModel.uiState.value.favoriteIds)
            assertEquals(setOf(7), viewModel.uiState.value.pendingProductIds)
            assertEquals(listOf(Triple(userId, 7, true)), repository.mutations)

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
            viewModel.onUserChanged(userId)
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
            viewModel.onUserChanged(userId)
            advanceUntilIdle()

            viewModel.toggleFavorite(12)
            viewModel.toggleFavorite(12)
            runCurrent()

            assertEquals(listOf(Triple(userId, 12, true)), repository.mutations)

            responseGate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `failed load keeps only the current user local IDs and reports the error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeFavoriteRepository(initialIds = setOf(5)).apply {
                loadError = IllegalStateException("No se pudieron cargar los favoritos (500)")
            }
            val viewModel = FavoriteViewModel(repository)

            viewModel.onUserChanged(userId)
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
    favorites: List<Product> = emptyList(),
    private val mutationGate: CompletableDeferred<Unit>? = null
) : FavoriteRepository {
    private val idsByUser = mutableMapOf(
        DEFAULT_USER_ID to MutableStateFlow(initialIds)
    )
    private val favoritesByUser = mutableMapOf(
        DEFAULT_USER_ID to favorites
    )

    val loadRequests = mutableListOf<Int>()
    val cancelledLoads = mutableListOf<Int>()
    val loadGates = mutableMapOf<Int, CompletableDeferred<Unit>>()
    val mutations = mutableListOf<Triple<Int, Int, Boolean>>()
    var loadError: Throwable? = null
    var mutationError: Throwable? = null

    fun seedUser(userId: Int, ids: Set<Int>, favorites: List<Product>) {
        idsByUser[userId] = MutableStateFlow(ids)
        favoritesByUser[userId] = favorites
    }

    override fun favoriteIds(userId: Int): Flow<Set<Int>> = idsFor(userId)

    override suspend fun loadFavorites(userId: Int): List<Product> {
        loadRequests += userId
        try {
            loadGates[userId]?.await()
        } catch (cancelled: CancellationException) {
            cancelledLoads += userId
            throw cancelled
        }
        loadError?.let { throw it }
        val favorites = favoritesByUser[userId].orEmpty()
        idsFor(userId).value = favorites.mapTo(mutableSetOf(), Product::id)
        return favorites
    }

    override suspend fun setFavorite(userId: Int, productId: Int, favorite: Boolean) {
        mutations += Triple(userId, productId, favorite)
        val ids = idsFor(userId)
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

    private fun idsFor(userId: Int): MutableStateFlow<Set<Int>> =
        idsByUser.getOrPut(userId) { MutableStateFlow(emptySet()) }

    private companion object {
        const val DEFAULT_USER_ID = 25
    }
}
