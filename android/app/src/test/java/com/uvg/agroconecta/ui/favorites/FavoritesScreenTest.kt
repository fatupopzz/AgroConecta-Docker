package com.uvg.agroconecta.ui.favorites

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.profile.FavoriteProductsButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FavoritesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `empty state explains how to save products`() {
        compose.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoriteUiState(),
                    onNavigateBack = {},
                    onProductClick = {},
                    onToggleFavorite = {},
                    onRetry = {},
                    onErrorShown = {}
                )
            }
        }

        compose.onNodeWithText("Todavía no tienes favoritos").assertIsDisplayed()
        compose.onNodeWithText("Toca el corazón de un producto para guardarlo aquí.")
            .assertIsDisplayed()
    }

    @Test
    fun `favorite product can open its detail or be removed`() {
        val product = product(7)
        var openedProductId: Int? = null
        var toggledProductId: Int? = null
        compose.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoriteUiState(
                        favoriteProducts = listOf(product),
                        favoriteIds = setOf(product.id)
                    ),
                    onNavigateBack = {},
                    onProductClick = { openedProductId = it },
                    onToggleFavorite = { toggledProductId = it },
                    onRetry = {},
                    onErrorShown = {}
                )
            }
        }

        compose.onNodeWithText(product.nombre).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Quitar de favoritos").assertIsDisplayed()
        compose.onNodeWithTag("favorite-button-7").performClick()

        assertEquals(7, openedProductId)
        assertEquals(7, toggledProductId)
    }

    @Test
    fun `error state offers retry`() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoriteUiState(errorMessage = "Sin conexión"),
                    onNavigateBack = {},
                    onProductClick = {},
                    onToggleFavorite = {},
                    onRetry = { retries += 1 },
                    onErrorShown = {}
                )
            }
        }

        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `pending favorite action is disabled`() {
        compose.setContent {
            MaterialTheme {
                FavoriteButton(
                    productId = 12,
                    isFavorite = true,
                    isPending = true,
                    onClick = {}
                )
            }
        }

        compose.onNodeWithTag("favorite-button-12").assertIsNotEnabled()
    }

    @Test
    fun `farmer profile action opens favorites`() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                FavoriteProductsButton(onClick = { clicks += 1 })
            }
        }

        compose.onNodeWithText("Mis favoritos").assertIsDisplayed().performClick()

        assertEquals(1, clicks)
    }

    private fun product(id: Int) = Product(
        id = id,
        nombre = "Producto $id",
        marca = "Agro Verde",
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
}
