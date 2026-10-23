package com.uvg.agroconecta.ui.diagnosis

import android.net.Uri
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.diagnosis.PlantDiagnosisResult
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseClassifier
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseLabel
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseLabels
import com.uvg.agroconecta.data.images.ProductImageReadResult
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.images.SelectedProductImage
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.CartAddResult
import com.uvg.agroconecta.data.repository.DiagnosisProducts
import com.uvg.agroconecta.data.repository.DiagnosisRepository
import com.uvg.agroconecta.data.repository.DiagnosisSubmission
import com.uvg.agroconecta.data.repository.DiagnosisUpload
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PlantDiagnosisViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Todos estrictos: si el ViewModel pide productos de una hoja sana o manda
    // un diagnóstico dudoso al backend, el test revienta por la llamada sin stub.
    private lateinit var imageReader: ProductImageReader
    private lateinit var classifier: PlantDiseaseClassifier
    private lateinit var repository: DiagnosisRepository
    private lateinit var locationProvider: CurrentLocationProvider
    private lateinit var connectivity: ConnectivityMonitor
    private lateinit var viewModel: PlantDiagnosisViewModel

    private val uri = mockk<Uri>()
    private val photo = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01)
    private val lateBlight = PlantDiseaseLabels.describe("Tomato___Late_blight")
    private val healthyLeaf = PlantDiseaseLabels.describe("Tomato___healthy")
    private val farm = GeoCoordinates(latitude = 14.63, longitude = -90.51)
    private val fungicide = product(id = 4, nombre = "Fungicida cúprico")

    @Before
    fun setUp() {
        imageReader = mockk()
        classifier = mockk()
        repository = mockk()
        locationProvider = mockk()
        connectivity = mockk()
        viewModel = PlantDiagnosisViewModel(imageReader, classifier, repository, locationProvider, connectivity)
        viewModel.start(userId = 7, farmerId = 12)
        coEvery { imageReader.read(uri) } returns
            ProductImageReadResult.Success(SelectedProductImage(uri, photo, "image/jpeg"))
    }

    @Test
    fun `diagnóstico exitoso se guarda con la ubicación y muestra productos`() =
        runTest(mainDispatcherRule.testDispatcher) {
            classifierReturns(PlantDiagnosisResult.Diagnosed(lateBlight, 0.91f))
            coEvery { locationProvider.getCurrentCoordinates() } returns farm
            coEvery { repository.submit(submissionFor(lateBlight, 0.91f, farm)) } returns DiagnosisUpload.Sent(15)
            coEvery { repository.recommendedProducts(15) } returns
                DiagnosisProducts(listOf(fungicide), listOf("oxicloruro de cobre"), "Aplicar cada 7 días")

            viewModel.onImageSelected(uri)
            advanceUntilIdle()

            val state = result()
            assertEquals("Tomate", state.label.crop)
            assertEquals("Tizón tardío", state.label.disease)
            assertEquals(0.91f, state.confidence)
            assertEquals(UploadState.Sent(15), state.upload)
            assertTrue(state.hasLocation)
            assertEquals(
                ProductsState.Loaded(listOf(fungicide), "Aplicar cada 7 días", listOf("oxicloruro de cobre")),
                state.products
            )
            coVerify(exactly = 1) { classifier.classify(match { it.contentEquals(photo) }) }
            coVerify(exactly = 1) { repository.submit(submissionFor(lateBlight, 0.91f, farm)) }
            coVerify(exactly = 1) { repository.recommendedProducts(15) }
            confirmVerified(repository)
        }

    @Test
    fun `con confianza baja pide otra foto y no guarda nada`() =
        runTest(mainDispatcherRule.testDispatcher) {
            classifierReturns(PlantDiagnosisResult.UnclearPhoto(bestConfidence = 0.42f))

            viewModel.onImageSelected(uri)
            advanceUntilIdle()

            assertEquals(PlantDiagnosisUiState.UnclearPhoto(0.42f), viewModel.uiState.value)
            confirmVerified(repository, locationProvider)

            viewModel.takeAnotherPhoto()
            assertEquals(PlantDiagnosisUiState.Capture, viewModel.uiState.value)
        }

    @Test
    fun `una hoja sana es un resultado positivo sin productos ni alerta`() =
        runTest(mainDispatcherRule.testDispatcher) {
            classifierReturns(PlantDiagnosisResult.Diagnosed(healthyLeaf, 0.97f))
            coEvery { locationProvider.getCurrentCoordinates() } returns farm
            coEvery { repository.submit(submissionFor(healthyLeaf, 0.97f, farm)) } returns DiagnosisUpload.Sent(16)

            viewModel.onImageSelected(uri)
            advanceUntilIdle()
            viewModel.alertNeighbors()
            advanceUntilIdle()

            val state = result()
            assertTrue(state.label.isHealthy)
            assertEquals("Hoja sana", state.label.disease)
            assertEquals(ProductsState.NotApplicable, state.products)
            assertEquals(AlertState.Idle, state.alert)
            coVerify(exactly = 1) { repository.submit(any()) }
            confirmVerified(repository)
        }

    @Test
    fun `sin conexión queda pendiente y se envía al volver la señal`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val online = MutableStateFlow(false)
            every { connectivity.status } returns online
            classifierReturns(PlantDiagnosisResult.Diagnosed(lateBlight, 0.88f))
            coEvery { locationProvider.getCurrentCoordinates() } returns farm
            coEvery { repository.submit(any()) } returns DiagnosisUpload.Pending("local-1")
            coEvery { repository.retryPending("local-1") } returns 20
            coEvery { repository.recommendedProducts(20) } returns
                DiagnosisProducts(listOf(fungicide), emptyList(), null)

            viewModel.onImageSelected(uri)
            advanceUntilIdle()

            // El diagnóstico se ve igual aunque no haya señal: el modelo corre en el teléfono.
            val offline = result()
            assertEquals("Tizón tardío", offline.label.disease)
            assertEquals(UploadState.Pending("local-1"), offline.upload)
            assertEquals(ProductsState.WaitingForSignal, offline.products)
            coVerify(exactly = 0) { repository.retryPending(any()) }

            online.value = true
            advanceUntilIdle()

            val synced = result()
            assertEquals(UploadState.Sent(20), synced.upload)
            assertEquals(ProductsState.Loaded(listOf(fungicide), null, emptyList()), synced.products)
            coVerify(exactly = 1) { repository.retryPending("local-1") }
        }

    @Test
    fun `un error de red al pedir productos permite reintentar`() =
        runTest(mainDispatcherRule.testDispatcher) {
            classifierReturns(PlantDiagnosisResult.Diagnosed(lateBlight, 0.9f))
            coEvery { locationProvider.getCurrentCoordinates() } returns farm
            coEvery { repository.submit(any()) } returns DiagnosisUpload.Sent(15)
            coEvery { repository.recommendedProducts(15) } throws IOException("sin red")

            viewModel.onImageSelected(uri)
            advanceUntilIdle()

            assertTrue(result().products is ProductsState.Error)
            assertEquals(UploadState.Sent(15), result().upload)

            coEvery { repository.recommendedProducts(15) } returns
                DiagnosisProducts(listOf(fungicide), emptyList(), null)
            viewModel.retryProducts()
            advanceUntilIdle()

            assertEquals(ProductsState.Loaded(listOf(fungicide), null, emptyList()), result().products)
            coVerify(exactly = 2) { repository.recommendedProducts(15) }
        }

    @Test
    fun `agrega un producto recomendado al carrito`() =
        runTest(mainDispatcherRule.testDispatcher) {
            diagnoseLateBlight()
            coEvery { repository.addToCart(farmerId = 12, productId = 4) } returns CartAddResult.Added("Agro Dos")

            viewModel.addToCart(fungicide)
            advanceUntilIdle()

            assertEquals("Fungicida cúprico agregado al carrito (Agro Dos)", viewModel.message.value)
            assertEquals(emptySet<Int>(), result().addingToCart)
            coVerify(exactly = 1) { repository.addToCart(12, 4) }
        }

    @Test
    fun `avisa si ningún distribuidor tiene existencias del producto`() =
        runTest(mainDispatcherRule.testDispatcher) {
            diagnoseLateBlight()
            coEvery { repository.addToCart(12, 4) } returns CartAddResult.NoStock

            viewModel.addToCart(fungicide)
            advanceUntilIdle()

            assertEquals("Ningún distribuidor tiene Fungicida cúprico disponible por ahora", viewModel.message.value)
        }

    @Test
    fun `avisar a mis vecinos publica la alerta una sola vez`() =
        runTest(mainDispatcherRule.testDispatcher) {
            diagnoseLateBlight()
            coEvery { repository.publishAlert(15) } returns Unit

            viewModel.alertNeighbors()
            advanceUntilIdle()
            viewModel.alertNeighbors()
            advanceUntilIdle()

            assertEquals(AlertState.Sent, result().alert)
            assertEquals(ALERT_SENT_MESSAGE, viewModel.message.value)
            coVerify(exactly = 1) { repository.publishAlert(15) }
        }

    @Test
    fun `sin permiso de ubicación diagnostica pero no deja avisar a los vecinos`() =
        runTest(mainDispatcherRule.testDispatcher) {
            classifierReturns(PlantDiagnosisResult.Diagnosed(lateBlight, 0.9f))
            coEvery { locationProvider.getCurrentCoordinates() } throws SecurityException("sin permiso")
            coEvery { repository.submit(submissionFor(lateBlight, 0.9f, location = null)) } returns
                DiagnosisUpload.Sent(15)
            coEvery { repository.recommendedProducts(15) } returns DiagnosisProducts(emptyList(), emptyList(), null)

            viewModel.onImageSelected(uri)
            advanceUntilIdle()
            viewModel.alertNeighbors()
            advanceUntilIdle()

            assertFalse(result().hasLocation)
            assertEquals(AlertState.Idle, result().alert)
            coVerify(exactly = 0) { repository.publishAlert(any()) }
        }

    @Test
    fun `una foto ilegible vuelve a la captura con el motivo`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { imageReader.read(uri) } returns ProductImageReadResult.Invalid("No se pudo usar esa imagen")

            viewModel.onImageSelected(uri)
            advanceUntilIdle()

            assertEquals(PlantDiagnosisUiState.Capture, viewModel.uiState.value)
            assertEquals("No se pudo usar esa imagen", viewModel.message.value)
            confirmVerified(classifier, repository)
        }

    @Test
    fun `cerrar la cámara sin tomar foto no cambia nada`() {
        viewModel.onImageSelected(null)

        assertEquals(PlantDiagnosisUiState.Capture, viewModel.uiState.value)
        assertNull(viewModel.message.value)
        confirmVerified(imageReader, classifier, repository)
    }

    private fun TestScope.diagnoseLateBlight() {
        classifierReturns(PlantDiagnosisResult.Diagnosed(lateBlight, 0.91f))
        coEvery { locationProvider.getCurrentCoordinates() } returns farm
        coEvery { repository.submit(any()) } returns DiagnosisUpload.Sent(15)
        coEvery { repository.recommendedProducts(15) } returns DiagnosisProducts(listOf(fungicide), emptyList(), null)
        viewModel.onImageSelected(uri)
        advanceUntilIdle()
    }

    private fun classifierReturns(result: PlantDiagnosisResult) {
        coEvery { classifier.classify(any()) } returns result
    }

    private fun result(): PlantDiagnosisUiState.Result =
        viewModel.uiState.value as PlantDiagnosisUiState.Result

    private fun submissionFor(
        label: PlantDiseaseLabel,
        confidence: Float,
        location: GeoCoordinates?
    ) = DiagnosisSubmission(
        userId = 7,
        crop = label.crop,
        diseaseLabel = label.label,
        confidence = confidence,
        latitude = location?.latitude,
        longitude = location?.longitude
    )

    private fun product(id: Int, nombre: String) = Product(
        id = id,
        nombre = nombre,
        marca = "Agro",
        descripcion = null,
        composicion = "oxicloruro de cobre 50 %",
        dosis = null,
        instrucciones = null,
        calificacion = 4.5,
        categoria = "Fungicidas",
        precioDesde = null,
        numDistribuidores = null
    )
}
