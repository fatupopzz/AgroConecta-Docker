package com.uvg.agroconecta.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProductCacheMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migracion-cache.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migra la cache v1 sin perder los productos guardados`() = runTest {
        // Tabla tal como la creaba Room en la versión 1, con un producto que un
        // usuario ya tenía guardado para ver el catálogo sin conexión.
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `producto_cache` (`id` INTEGER NOT NULL, " +
                    "`nombre` TEXT NOT NULL, `categoria` TEXT, `precio` REAL, " +
                    "`descripcion` TEXT, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "INSERT INTO producto_cache (id, nombre, categoria, precio, descripcion, timestamp) " +
                    "VALUES (1, 'Abono foliar', 'Fertilizantes', 42.5, 'Desc', 1000)"
            )
            db.version = 1
        }

        val database = openWithMigrations()

        try {
            assertEquals(
                listOf(
                    ProductCacheEntity(
                        id = 1,
                        nombre = "Abono foliar",
                        categoria = "Fertilizantes",
                        precio = 42.5,
                        descripcion = "Desc",
                        timestamp = 1000L,
                        imagenUrl = null,
                        imagenThumbUrl = null
                    )
                ),
                database.productCacheDao().getAll()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `migra la cache v2 conservando la foto y sin miniatura`() = runTest {
        // Tabla de la versión 2: ya tenía imagenUrl pero no la miniatura.
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `producto_cache` (`id` INTEGER NOT NULL, " +
                    "`nombre` TEXT NOT NULL, `categoria` TEXT, `precio` REAL, " +
                    "`descripcion` TEXT, `timestamp` INTEGER NOT NULL, `imagenUrl` TEXT, " +
                    "PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "INSERT INTO producto_cache " +
                    "(id, nombre, categoria, precio, descripcion, timestamp, imagenUrl) " +
                    "VALUES (2, 'Semilla', 'Semillas', 10.0, NULL, 2000, '/uploads/productos/s.jpg')"
            )
            db.version = 2
        }

        val database = openWithMigrations()

        try {
            val cached = database.productCacheDao().getAll().single()
            assertEquals("/uploads/productos/s.jpg", cached.imagenUrl)
            assertNull(cached.imagenThumbUrl)
        } finally {
            database.close()
        }
    }

    @Test
    fun `migra v3 conserva miniatura y persiste siembras separadas al reabrir`() = runTest {
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE producto_cache (id INTEGER NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, categoria TEXT, precio REAL, descripcion TEXT, timestamp INTEGER NOT NULL, imagenUrl TEXT, imagenThumbUrl TEXT)")
            db.execSQL("INSERT INTO producto_cache VALUES (3, 'Semilla', NULL, 10.0, NULL, 1000, '/s.jpg', '/thumb.webp')")
            db.version = 3
        }
        val first = openWithMigrations()
        try {
            val db = first
            assertEquals("/thumb.webp", db.productCacheDao().getAll().single().imagenThumbUrl)
            db.plantingCacheDao().put(PlantingCacheEntity(7, "[{siembra:1}]"))
            db.plantingCacheDao().put(PlantingCacheEntity(8, "[]"))
        } finally { first.close() }
        val reopened = openWithMigrations()
        try {
            val db = reopened
            assertEquals("[{siembra:1}]", db.plantingCacheDao().get(7)?.payload)
            assertEquals("[]", db.plantingCacheDao().get(8)?.payload)
            assertNull(db.plantingCacheDao().get(9))
        } finally { reopened.close() }
    }

    private fun openWithMigrations(): AgroConectaDatabase =
        Room.databaseBuilder(context, AgroConectaDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
}
