package com.uvg.agroconecta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductCacheEntity::class, PlantingCacheEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AgroConectaDatabase : RoomDatabase() {
    abstract fun plantingCacheDao(): PlantingCacheDao
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

/** v3 agrega la miniatura que usan las tarjetas del catálogo sin conexión. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE producto_cache ADD COLUMN imagenThumbUrl TEXT")
    }
}

/** Conserva la caché del catálogo y agrega snapshots de siembras por usuario. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `planting_cache` (`userId` INTEGER NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`userId`))")
    }
}
