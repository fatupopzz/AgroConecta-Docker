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
    val timestamp: Long,
    // Se cachea para que el catálogo offline siga mostrando las fotos que Coil
    // ya tiene en disco; sin la ruta no habría forma de pedírselas.
    val imagenUrl: String? = null,
    val imagenThumbUrl: String? = null
)
