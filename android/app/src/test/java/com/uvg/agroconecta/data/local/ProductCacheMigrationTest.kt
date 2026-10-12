package com.uvg.agroconecta.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

        val database = Room.databaseBuilder(context, AgroConectaDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

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
                        imagenUrl = null
                    )
                ),
                database.productCacheDao().getAll()
            )
        } finally {
            database.close()
        }
    }
}
