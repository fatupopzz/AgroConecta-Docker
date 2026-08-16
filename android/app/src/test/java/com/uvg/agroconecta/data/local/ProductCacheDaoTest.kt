package com.uvg.agroconecta.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProductCacheDaoTest {

    private lateinit var database: AgroConectaDatabase
    private lateinit var dao: ProductCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AgroConectaDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.productCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inserts and reads cached products`() = runTest {
        val products = listOf(entity(id = 1), entity(id = 2))

        dao.insertAll(products)

        assertEquals(products, dao.getAll())
    }

    @Test
    fun `replaces all cached products atomically`() = runTest {
        dao.insertAll(listOf(entity(id = 1), entity(id = 2)))
        val replacement = entity(id = 3, name = "Semilla de maíz")

        dao.replaceAll(listOf(replacement))

        assertEquals(listOf(replacement), dao.getAll())
    }

    private fun entity(
        id: Int,
        name: String = "Producto $id"
    ) = ProductCacheEntity(
        id = id,
        nombre = name,
        categoria = "Semillas",
        precio = 42.5,
        descripcion = "Descripción",
        timestamp = 1_000L
    )
}
