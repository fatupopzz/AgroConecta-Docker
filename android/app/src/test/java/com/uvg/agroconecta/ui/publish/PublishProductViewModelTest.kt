package com.uvg.agroconecta.ui.publish

import android.net.Uri
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.images.ProductImageReadResult
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.images.ProductImageRules
import com.uvg.agroconecta.data.images.SelectedProductImage
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.CreateProductResponse
import com.uvg.agroconecta.data.models.ProductImageInfo
import com.uvg.agroconecta.data.models.ProductoCreado
import com.uvg.agroconecta.data.models.UploadProductImageResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PublishProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Mocks estrictos a propósito: una llamada que no esté stubbeada revienta el
     * test. Así se nota si la foto se sube antes del inventario o si se intenta
     * subir cuando no había ninguna elegida.
     */
    private lateinit var api: ApiService
    private lateinit var imageReader: ProductImageReader
    private lateinit var viewModel: PublishProductViewModel

    private val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    @Before
    fun setUp() {
        api = mockk()
        imageReader = mockk()
        coEvery { api.getCategories() } returns Response.success(
            listOf(Category(id = 3, nombre = "Fertilizantes", descripcion = null))
        )
        viewModel = PublishProductViewModel(api, imageReader)
    }

    @Test
    fun `carga las categorias al iniciar`() {
        assertEquals(listOf("Fertilizantes"), viewModel.uiState.value.categorias.map { it.nombre })
        assertFalse(viewModel.uiState.value.isLoadingCategorias)
    }

    @Test
    fun `sin foto publica producto e inventario y no intenta subir imagen`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubProductCreated(idProducto = 42)
            stubInventoryCreated()

            publish()
            advanceUntilIdle()

            assertSame(PublishState.Success, viewModel.uiState.value.publishState)
            coVerify(exactly = 0) { api.uploadProductImage(any(), any()) }
        }

    @Test
    fun `con foto la sube al final con el id del producto creado`() =
        runTest(mainDispatcherRule.testDispatcher) {
            selectImage(mimeType = "image/png")
            stubProductCreated(idProducto = 42)
            stubInventoryCreated()
            val part = slot<MultipartBody.Part>()
            coEvery { api.uploadProductImage(42, capture(part)) } returns Response.success(
                UploadProductImageResponse(
                    message = "Imagen del producto actualizada",
                    producto = ProductImageInfo(42, "/uploads/productos/abc.png")
                )
            )

            publish()
            advanceUntilIdle()

            assertSame(PublishState.Success, viewModel.uiState.value.publishState)
            // El backend rechaza la foto de quien todavía no tiene el producto
            // en su inventario, así que el orden importa.
            coVerifyOrder {
                api.createProduct(any())
                api.createInventory(any())
                api.uploadProductImage(42, any())
            }
            val disposition = part.captured.headers?.get("Content-Disposition").orEmpty()
            assertTrue(disposition.contains("name=\"imagen\""))
            assertTrue(disposition.contains("filename=\"producto.png\""))
            assertEquals("image/png", part.captured.body.contentType().toString())
            val sentBytes = Buffer().also { part.captured.body.writeTo(it) }.readByteArray()
            assertArrayEquals(pngBytes, sentBytes)
        }

    @Test
    fun `si el servidor rechaza la foto el producto queda publicado sin imagen`() =
        runTest(mainDispatcherRule.testDispatcher) {
            selectImage()
            stubProductCreated(idProducto = 42)
            stubInventoryCreated()
            coEvery { api.uploadProductImage(42, any()) } returns Response.error(
                413,
                """{"error":"La imagen supera el máximo de 5 MB"}"""
                    .toResponseBody("application/json".toMediaTypeOrNull())
            )

            publish()
            advanceUntilIdle()

            val state = viewModel.uiState.value.publishState
            assertTrue(state is PublishState.PublishedWithoutImage)
            assertEquals(
                "Producto publicado, pero no se pudo subir la foto: supera el máximo de 5 MB",
                (state as PublishState.PublishedWithoutImage).message
            )
        }

    @Test
    fun `un fallo de red al subir la foto no se reporta como error de publicacion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            selectImage()
            stubProductCreated(idProducto = 42)
            stubInventoryCreated()
            coEvery { api.uploadProductImage(42, any()) } throws IOException("timeout")

            publish()
            advanceUntilIdle()

            // Con un Error la pantalla se quedaría abierta y el distribuidor
            // volvería a publicar, duplicando el producto.
            val state = viewModel.uiState.value.publishState
            assertTrue(state is PublishState.PublishedWithoutImage)
        }

    @Test
    fun `si falla el inventario no se intenta subir la foto`() =
        runTest(mainDispatcherRule.testDispatcher) {
            selectImage()
            stubProductCreated(idProducto = 42)
            coEvery { api.createInventory(any()) } returns Response.error(
                403,
                "{}".toResponseBody("application/json".toMediaTypeOrNull())
            )

            publish()
            advanceUntilIdle()

            val state = viewModel.uiState.value.publishState
            assertEquals(
                PublishState.Error("Producto creado pero error en inventario (403)"),
                state
            )
            coVerify(exactly = 0) { api.uploadProductImage(any(), any()) }
        }

    @Test
    fun `si falla la creacion del producto no sigue con inventario ni foto`() =
        runTest(mainDispatcherRule.testDispatcher) {
            selectImage()
            coEvery { api.createProduct(any()) } returns Response.error(
                400,
                "{}".toResponseBody("application/json".toMediaTypeOrNull())
            )

            publish()
            advanceUntilIdle()

            assertEquals(
                PublishState.Error("Error al crear producto (400)"),
                viewModel.uiState.value.publishState
            )
            coVerify(exactly = 0) { api.createInventory(any()) }
            coVerify(exactly = 0) { api.uploadProductImage(any(), any()) }
        }

    @Test
    fun `una foto invalida muestra el error y conserva la anterior`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val anterior = selectImage()
            val otraUri = mockk<Uri>()
            coEvery { imageReader.read(otraUri) } returns
                ProductImageReadResult.Invalid(ProductImageRules.TOO_LARGE_MESSAGE)

            viewModel.onImageSelected(otraUri)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ProductImageRules.TOO_LARGE_MESSAGE, state.imageError)
            assertSame(anterior, state.imagen)
            assertFalse(state.isReadingImage)
        }

    @Test
    fun `cerrar el picker sin elegir no lee nada ni borra la foto`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val anterior = selectImage()

            viewModel.onImageSelected(null)
            advanceUntilIdle()

            assertSame(anterior, viewModel.uiState.value.imagen)
            coVerify(exactly = 1) { imageReader.read(any()) }
        }

    @Test
    fun `quitar la foto evita que se suba al publicar`() =
        runTest(mainDispatcherRule.testDispatcher) {
            selectImage()
            viewModel.removeImage()
            stubProductCreated(idProducto = 42)
            stubInventoryCreated()

            publish()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.imagen)
            assertSame(PublishState.Success, viewModel.uiState.value.publishState)
            coVerify(exactly = 0) { api.uploadProductImage(any(), any()) }
        }

    @Test
    fun `clearImageError limpia el mensaje ya mostrado`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val uri = mockk<Uri>()
            coEvery { imageReader.read(uri) } returns
                ProductImageReadResult.Invalid(ProductImageRules.UNREADABLE_MESSAGE)
            viewModel.onImageSelected(uri)
            advanceUntilIdle()

            viewModel.clearImageError()

            assertNull(viewModel.uiState.value.imageError)
        }

    private fun selectImage(mimeType: String = "image/png"): SelectedProductImage {
        val uri = mockk<Uri>()
        val image = SelectedProductImage(previewUri = uri, bytes = pngBytes, mimeType = mimeType)
        coEvery { imageReader.read(uri) } returns ProductImageReadResult.Success(image)
        viewModel.onImageSelected(uri)
        return image
    }

    private fun stubProductCreated(idProducto: Int) {
        coEvery { api.createProduct(any()) } returns Response.success(
            CreateProductResponse(
                message = "Producto creado correctamente",
                producto = ProductoCreado(idProducto = idProducto, nombre = "Abono foliar")
            )
        )
    }

    private fun stubInventoryCreated() {
        coEvery { api.createInventory(any()) } returns Response.success(
            mapOf<String, Any>("message" to "Inventario publicado correctamente")
        )
    }

    private fun publish() {
        viewModel.publishProduct(
            nombre = "Abono foliar",
            marca = "",
            descripcion = "",
            idCategoria = 3,
            precio = 85.0,
            stock = 10,
            unidadMedida = "L",
            tiempoEntrega = 2
        )
    }
}
