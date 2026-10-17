package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

data class PlantingCategory(@SerializedName("id_categoria") val id: Int, val nombre: String)
data class PlantingTemplate(
    @SerializedName("id_plantilla") val id: Int,
    val cultivo: String,
    val nombre: String,
    val descripcion: String,
    @SerializedName("duracion_dias") val durationDays: Int
)
data class PlantingTask(
    @SerializedName("id_tarea") val id: Int,
    val nombre: String,
    val tipo: String,
    @SerializedName("fecha_programada") val date: String,
    @SerializedName("categoria_sugerida") val category: PlantingCategory?,
    @SerializedName("hecha") val completed: Boolean,
    @SerializedName("fecha_hecha") val completedAt: String? = null
)
data class Planting(
    @SerializedName("id_siembra") val id: Int,
    val cultivo: String,
    @SerializedName("nombre_cultivo") val name: String,
    @SerializedName("fecha_siembra") val date: String,
    @SerializedName("tareas") val tasks: List<PlantingTask>
)
data class CreatePlantingRequest(val cultivo: String, @SerializedName("fecha_siembra") val date: String)
data class UpdatePlantingTaskRequest(@SerializedName("hecha") val completed: Boolean)
data class PlantingTaskStatus(
    @SerializedName("id_tarea") val id: Int,
    @SerializedName("id_siembra") val plantingId: Int,
    @SerializedName("hecha") val completed: Boolean,
    @SerializedName("fecha_hecha") val completedAt: String?
)
