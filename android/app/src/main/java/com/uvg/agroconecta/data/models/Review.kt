package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

data class Review(
    @SerializedName("id_resena")          val id: Int,
    @SerializedName("calificacion")       val calificacion: Int,
    @SerializedName("comentario")         val comentario: String?,
    @SerializedName("fecha_resena")       val fechaCreacion: String?,
    @SerializedName("agricultor_nombre")  val nombreAgricultor: String?
)

data class ReviewsResponse(
    @SerializedName("promedio")  val promedio: Double?,
    @SerializedName("total")     val total: Int,
    @SerializedName("resenas")   val reviews: List<Review>   // backend devuelve "resenas"
)

data class CreateReviewRequest(
    @SerializedName("calificacion") val calificacion: Int,
    @SerializedName("comentario")   val comentario: String
)