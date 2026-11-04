package com.uvg.agroconecta.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserRoleTest {
    @Test
    fun `convierte todos los roles enviados por el backend`() {
        assertEquals(UserRole.FARMER, UserRole.fromBackend("agricultor"))
        assertEquals(UserRole.DISTRIBUTOR, UserRole.fromBackend("distribuidor"))
        assertEquals(UserRole.ADMIN, UserRole.fromBackend("administrador"))
    }

    @Test
    fun `tolera mayusculas y espacios del backend`() {
        assertEquals(UserRole.FARMER, UserRole.fromBackend("  AGRICULTOR "))
        assertEquals(UserRole.DISTRIBUTOR, UserRole.fromBackend("Distribuidor"))
        assertEquals(UserRole.ADMIN, UserRole.fromBackend("ADMINISTRADOR"))
    }

    @Test
    fun `valor desconocido no rompe la aplicacion`() {
        assertNull(UserRole.fromBackend("cliente"))
        assertNull(UserRole.fromBackend(""))
        assertNull(UserRole.fromBackend("   "))
        assertNull(UserRole.fromBackend(null))
    }

    @Test
    fun `expone los valores exactos esperados por el backend`() {
        assertEquals("agricultor", UserRole.FARMER.apiValue)
        assertEquals("distribuidor", UserRole.DISTRIBUTOR.apiValue)
        assertEquals("administrador", UserRole.ADMIN.apiValue)
    }
}
