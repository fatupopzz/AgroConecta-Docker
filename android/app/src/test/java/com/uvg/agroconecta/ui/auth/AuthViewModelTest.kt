package com.uvg.agroconecta.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.TipoCuenta
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel()
    }

    @Test
    fun `estado inicial de login es Idle`() {
        assertEquals(AuthState.Idle, viewModel.loginState.value)
    }

    @Test
    fun `estado inicial de registro es Idle`() {
        assertEquals(AuthState.Idle, viewModel.registerState.value)
    }

    @Test
    fun `nombre usuario inicia vacio`() {
        assertEquals("", viewModel.nombreUsuario.value)
    }

    @Test
    fun `draft inicial tiene tipo agricultor por defecto`() {
        assertEquals(TipoCuenta.AGRICULTOR, viewModel.registerDraft.value?.tipoCuenta)
    }

    @Test
    fun `updateDraft actualiza nombre correctamente`() {
        viewModel.updateDraft { it.copy(nombre = "Fatima") }
        assertEquals("Fatima", viewModel.registerDraft.value?.nombre)
    }

    @Test
    fun `updateDraft actualiza email correctamente`() {
        viewModel.updateDraft { it.copy(email = "fatima@test.com") }
        assertEquals("fatima@test.com", viewModel.registerDraft.value?.email)
    }

    @Test
    fun `updateDraft cambia tipo de cuenta a distribuidor`() {
        viewModel.updateDraft { it.copy(tipoCuenta = TipoCuenta.DISTRIBUIDOR) }
        assertEquals(TipoCuenta.DISTRIBUIDOR, viewModel.registerDraft.value?.tipoCuenta)
    }

    @Test
    fun `updateDraft actualiza multiples campos`() {
        viewModel.updateDraft {
            it.copy(
                nombre = "Juan",
                email = "juan@test.com",
                telefono = "55551234",
                departamento = "Guatemala"
            )
        }
        val draft = viewModel.registerDraft.value
        assertNotNull(draft)
        assertEquals("Juan", draft?.nombre)
        assertEquals("juan@test.com", draft?.email)
        assertEquals("55551234", draft?.telefono)
        assertEquals("Guatemala", draft?.departamento)
    }

    @Test
    fun `resetRegister limpia draft y restaura estado`() {
        viewModel.updateDraft { it.copy(nombre = "Test", email = "test@test.com") }
        viewModel.resetRegister()

        val draft = viewModel.registerDraft.value
        assertEquals("", draft?.nombre)
        assertEquals("", draft?.email)
        assertEquals(AuthState.Idle, viewModel.registerState.value)
    }

    @Test
    fun `resetLogin restaura estado a Idle y limpia nombre`() {
        viewModel.resetLogin()
        assertEquals(AuthState.Idle, viewModel.loginState.value)
        assertEquals("", viewModel.nombreUsuario.value)
    }
}
