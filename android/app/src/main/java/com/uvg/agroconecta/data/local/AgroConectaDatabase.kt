package com.uvg.agroconecta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AgroConectaDatabase : RoomDatabase() {
    abstract fun productCacheDao(): ProductCacheDao
}

/**
 * v2 agrega imagenUrl a la caché del catálogo. Se migra en lugar de usar
 * fallbackToDestructiveMigration para no dejar ese comodín activo: el día que
 * esta base guarde algo que no sea caché, un cambio de versión lo borraría
 * sin avisar.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE producto_cache ADD COLUMN imagenUrl TEXT")
    }
}
