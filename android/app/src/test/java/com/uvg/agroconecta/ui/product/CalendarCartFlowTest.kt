package com.uvg.agroconecta.ui.product

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.*
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class CalendarCartFlowTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val api = mockk<ApiService>()
    private val vm = ProductViewModel(api)
    private val offer = DistributorOffer(12, 20.0, 5, "unidad", 3, "Distribuidor", 4.0, "verificado")
    private val product = ProductDetail(4, "Semillas", null, null, null, null, null, 4.0, "Semillas", listOf(offer))
    @Test fun `calendario exige seleccion explicita de inventario real`() {
        coEvery { api.getProductById(4) } returns Response.success(product)
        vm.loadProduct(4, requireOfferSelection = true)
        assertNull(vm.selectedOffer.value)
        vm.addToCart(7)
        assertNotNull(vm.error.value)
        coVerify(exactly = 1) { api.getProductById(4) }; confirmVerified(api)
    }
    @Test fun `flujo normal conserva oferta de menor precio`() {
        coEvery { api.getProductById(4) } returns Response.success(product)
        vm.loadProduct(4)
        assertEquals(offer, vm.selectedOffer.value)
        coVerify(exactly = 1) { api.getProductById(4) }; confirmVerified(api)
    }
    @Test fun `agrega inventario elegido una sola vez y confirma`() = runTest {
        val gate = CompletableDeferred<Response<Map<String, Any>>>()
        coEvery { api.addToCart(7, AddItemRequest(12, 1)) } coAnswers { gate.await() }
        vm.selectOffer(offer); vm.addToCart(7); vm.addToCart(7)
        assertTrue(vm.isAddingToCart.value == true)
        gate.complete(Response.success(emptyMap()))
        assertNotNull(vm.cartSuccess.value); assertFalse(vm.isAddingToCart.value == true)
        coVerify(exactly = 1) { api.addToCart(7, AddItemRequest(12, 1)) }; confirmVerified(api)
    }
    @Test fun `error al agregar permite reintentar`() {
        coEvery { api.addToCart(7, AddItemRequest(12, 1)) } throws IOException()
        vm.selectOffer(offer); vm.addToCart(7)
        assertNotNull(vm.error.value); assertNull(vm.cartSuccess.value)
        assertFalse(vm.isAddingToCart.value == true)
        coEvery { api.addToCart(7, AddItemRequest(12, 1)) } returns Response.success(emptyMap())
        vm.addToCart(7)
        assertNotNull(vm.cartSuccess.value); assertNull(vm.error.value)
        coVerify(exactly = 2) { api.addToCart(7, AddItemRequest(12, 1)) }; confirmVerified(api)
    }
    @Test fun `no envia inventario invalido sin existencias ni usuario invalido`() {
        vm.selectOffer(offer.copy(idInventario = 0)); vm.addToCart(7)
        vm.selectOffer(offer.copy(stock = 0)); vm.addToCart(7)
        vm.selectOffer(offer); vm.addToCart(-1)
        confirmVerified(api)
    }
}
