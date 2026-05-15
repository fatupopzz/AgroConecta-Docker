package com.uvg.agroconecta.data.models

data class Review(
    val id: Int,
    val idProducto: Int,
    val idAgricultor: Int,
    val nombreAgricultor: String?,
    val calificacion: Int,        // 1-5
    val comentario: String?,
    val fechaCreacion: String
)

data class ReviewsResponse(
    val reviews: List<Review>,
    val promedio: Double?,
    val total: Int
)

data class CreateReviewRequest(
    val calificacion: Int,
    val comentario: String
)