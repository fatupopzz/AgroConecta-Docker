package com.uvg.agroconecta.data.local

import androidx.room.*

/** Snapshot del servidor. La clave impide mezclar calendarios entre sesiones. */
@Entity(tableName = "planting_cache")
data class PlantingCacheEntity(@PrimaryKey val userId: Int, val payload: String)

@Dao
interface PlantingCacheDao {
    @Query("SELECT * FROM planting_cache WHERE userId = :userId")
    suspend fun get(userId: Int): PlantingCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(snapshot: PlantingCacheEntity)
}
