package com.uvg.agroconecta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProductCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AgroConectaDatabase : RoomDatabase() {
    abstract fun productCacheDao(): ProductCacheDao
}
