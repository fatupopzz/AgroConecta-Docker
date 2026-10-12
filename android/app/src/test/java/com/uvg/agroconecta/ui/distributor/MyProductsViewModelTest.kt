package com.uvg.agroconecta.ui.distributor

import android.net.Uri
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.images.ProductImageReadResult
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.images.ProductImageRules
import com.uvg.agroconecta.data.images.SelectedProductImage
import com.uvg.agroconecta.data.models.DistributorInventoryResponse
import com.uvg.agroconecta.data.models.InventoryOwner
import com.uvg.agroconecta.data.models.InventoryProduct
import com.uvg.agroconecta.data.models.ProductImageInfo
import com.uvg.agroconecta.data.models.UploadProductImageResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MyProductsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Estrictos: si la pantalla intentara subir una foto sin que el usuario la
    // haya elegido, o recargar toda la lista tras cada subida, el test revienta.
    private lateinit var api: ApiService
    private lateinit var imageReader: ProductImageReader

    private val abono = product(idInventario = 10, idProducto = 3, nombre = "Abono foliar")
    private val semilla = product(idInventario = 11, idProducto = 7, nombre = "Semilla de maíz")

    @Before
    fun setUp() {
        api = mockk()
        imageReader = mockk()
    }

    @Test
    fun `carga los productos publicados del distribuidor`() = runTest(mainDispatcherRule.testDispatcher) {
        stubInventory(abono, semilla)

        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Abono foliar", "Semilla de maíz"), state.products.map { it.producto })
        assertEquals(false, state.isLoading)
        assertNull(state.loadError)
    }

    @Test
    fun `con la cuenta sin verificar avisa y no abre el picker`() = runTest(mainDispatcherRule.testDispatcher) {
        stubInventory(abono, estado = "pendiente")
        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isVerified)

        val canPick = viewModel.onChangePhotoClick(3)

        assertFalse(canPick)
        assertNull(viewModel.uiState.value.pendingPhotoProductId)
        assertEquals(UNVERIFIED_ACCOUNT_MESSAGE, viewModel.uiState.value.message)
        // La lista se sigue mostrando: el distribuidor ve sus productos aunque
        // todavía no pueda cambiar las fotos.
        assertEquals(listOf(abono), viewModel.uiState.value.products)
    }

    @Test
    fun `si el backend no manda el estado no se bloquea al distribuidor`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubInventory(abono, estado = null)
            val viewModel = MyProductsViewModel(api, imageReader)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isVerified)
            assertTrue(viewModel.onChangePhotoClick(3))
            assertEquals(3, viewModel.uiState.value.pendingPhotoProductId)
        }

    @Test
    fun `un usuario que no es distribuidor ve un mensaje claro`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { api.getMyInventory() } returns Response.error(
            403,
            "{}".toResponseBody("application/json".toMediaTypeOrNull())
        )

        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()

        assertEquals(
            "Solo los distribuidores tienen productos publicados",
            viewModel.uiState.value.loadError
        )
    }

    @Test
    fun `sin conexion muestra error y permite reintentar`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { api.getMyInventory() } throws IOException("sin red")
        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()
        assertEquals(
            "No se pudieron cargar tus productos. Revisa tu conexión.",
            viewModel.uiState.value.loadError
        )

        stubInventory(abono)
        viewModel.loadProducts()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadError)
        assertEquals(listOf(abono), viewModel.uiState.value.products)
    }

    @Test
    fun `cambiar la foto la sube y actualiza solo ese producto`() = runTest(mainDispatcherRule.testDispatcher) {
        stubInventory(abono, semilla)
        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()
        val uri = stubValidImage()
        coEvery { api.uploadProductImage(3, any()) } returns Response.success(
            UploadProductImageResponse(
                message = "Imagen del producto actualizada",
                producto = ProductImageInfo(3, "/uploads/productos/nueva.png")
            )
        )

        viewModel.onChangePhotoClick(3)
        viewModel.onImagePicked(uri)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("/uploads/productos/nueva.png", state.products.first { it.idProducto == 3 }.imagenUrl)
        assertNull(state.products.first { it.idProducto == 7 }.imagenUrl)
        assertEquals("Foto actualizada", state.message)
        assertNull(state.uploadingProductId)
        assertNull(state.pendingPhotoProductId)
        // Una sola carga de la lista: la del inicio.
        coVerify(exactly = 1) { api.getMyInventory() }
    }

    @Test
    fun `cerrar el picker sin elegir no sube nada`() = runTest(mainDispatcherRule.testDispatcher) {
        stubInventory(abono)
        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()

        viewModel.onChangePhotoClick(3)
        viewModel.onImagePicked(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingPhotoProductId)
        coVerify(exactly = 0) { imageReader.read(any()) }
        coVerify(exactly = 0) { api.uploadProductImage(any(), any()) }
    }

    @Test
    fun `una foto invalida se rechaza sin llamar al backend`() = runTest(mainDispatcherRule.testDispatcher) {
        stubInventory(abono)
        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()
        val uri = mockk<Uri>()
        coEvery { imageReader.read(uri) } returns
            ProductImageReadResult.Invalid(ProductImageRules.TOO_LARGE_MESSAGE)

        viewModel.onChangePhotoClick(3)
        viewModel.onImagePicked(uri)
        advanceUntilIdle()

        assertEquals(ProductImageRules.TOO_LARGE_MESSAGE, viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.products.first().imagenUrl)
        coVerify(exactly = 0) { api.uploadProductImage(any(), any()) }
    }

    @Test
    fun `si el backend rechaza la foto explica el motivo y no cambia la lista`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubInventory(abono)
            val viewModel = MyProductsViewModel(api, imageReader)
            advanceUntilIdle()
            val uri = stubValidImage()
            coEvery { api.uploadProductImage(3, any()) } returns Response.error(
                403,
                "{}".toResponseBody("application/json".toMediaTypeOrNull())
            )

            viewModel.onChangePhotoClick(3)
            viewModel.onImagePicked(uri)
            advanceUntilIdle()

            assertEquals(
                "No se pudo cambiar la foto: no tienes permiso sobre este producto",
                viewModel.uiState.value.message
            )
            assertEquals(listOf(abono), viewModel.uiState.value.products)
        }

    @Test
    fun `mientras una foto sube se ignora otra eleccion`() = runTest(mainDispatcherRule.testDispatcher) {
        stubInventory(abono, semilla)
        val viewModel = MyProductsViewModel(api, imageReader)
        advanceUntilIdle()
        val uri = stubValidImage()
        val pendingUpload = CompletableDeferred<Response<UploadProductImageResponse>>()
        coEvery { api.uploadProductImage(3, any()) } coAnswers { pendingUpload.await() }

        viewModel.onChangePhotoClick(3)
        viewModel.onImagePicked(uri)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.uploadingProductId)

        viewModel.onChangePhotoClick(7)
        viewModel.onImagePicked(uri)
        advanceUntilIdle()

        coVerify(exactly = 0) { api.uploadProductImage(7, any()) }
        pendingUpload.complete(
            Response.success(
                UploadProductImageResponse("ok", ProductImageInfo(3, "/uploads/productos/a.png"))
            )
        )
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.uploadingProductId)
    }

    private fun stubInventory(vararg products: InventoryProduct, estado: String? = "verificado") {
        coEvery { api.getMyInventory() } returns Response.success(
            DistributorInventoryResponse(
                inventario = products.toList(),
                distribuidor = estado?.let { InventoryOwner(estadoVerificacion = it) }
            )
        )
    }

    private fun stubValidImage(): Uri {
        val uri = mockk<Uri>()
        val image = SelectedProductImage(
            previewUri = uri,
            bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
            mimeType = "image/png"
        )
        coEvery { imageReader.read(uri) } returns ProductImageReadResult.Success(image)
        return uri
    }

    private fun product(idInventario: Int, idProducto: Int, nombre: String) = InventoryProduct(
        idInventario = idInventario,
        idProducto = idProducto,
        producto = nombre,
        marca = null,
        categoria = "Fertilizantes",
        precio = 85.0,
        stock = 10,
        unidadMedida = "L"
    )
}
