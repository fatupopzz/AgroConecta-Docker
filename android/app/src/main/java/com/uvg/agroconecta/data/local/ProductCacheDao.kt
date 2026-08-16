package com.uvg.agroconecta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductCacheDao {

    @Query("SELECT * FROM producto_cache ORDER BY id")
    abstract fun observeAll(): Flow<List<ProductCacheEntity>>

    @Query("SELECT * FROM producto_cache ORDER BY id")
    abstract suspend fun getAll(): List<ProductCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(products: List<ProductCacheEntity>)

    @Query("DELETE FROM producto_cache")
    abstract suspend fun deleteAll()

    @Query("SELECT MIN(timestamp) FROM producto_cache")
    abstract suspend fun oldestTimestamp(): Long?

    @Transaction
    open suspend fun replaceAll(products: List<ProductCacheEntity>) {
        deleteAll()
        if (products.isNotEmpty()) {
            insertAll(products)
        }
    }
}
