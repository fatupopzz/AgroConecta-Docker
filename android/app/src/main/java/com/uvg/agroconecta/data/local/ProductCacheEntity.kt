package com.uvg.agroconecta.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "producto_cache")
data class ProductCacheEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val categoria: String?,
    val precio: Double?,
    val descripcion: String?,
    val timestamp: Long
)
