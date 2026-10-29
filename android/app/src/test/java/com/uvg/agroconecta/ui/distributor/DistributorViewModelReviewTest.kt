package com.uvg.agroconecta.ui.distributor

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CreateReviewRequest
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.data.models.DistributorReviewCreatedResponse
import com.uvg.agroconecta.data.models.DistributorReviewsResponse
import com.uvg.agroconecta.data.models.RatingDistribution
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class DistributorViewModelReviewTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock estricto: si el ViewModel volviera a reseñar un producto en lugar
    // del distribuidor, la llamada a createReview no está preparada y el test falla.
    private val api = mockk<ApiService>()

    private val rating = DistributorRatingResponse(
        idDistribuidor = 4,
        calificacionPromedio = 4.5,
        totalResenas = 2,
        distribucion = RatingDistribution(1, 1, 0, 0, 0)
    )

    private val reviews = DistributorReviewsResponse(
        page = 1,
        limit = 20,
        total = 1,
        totalPages = 1,
        reviews = listOf(
            DistributorReview(
                idResena = 9,
                calificacion = 5,
                comentario = "Entregó a tiempo",
                fechaResena = "2026-10-20T10:00:00Z",
                agricultorNombre = "María"
            )
        )
    )

    @Test
    fun `la reseña se guarda en el distribuidor y refresca su calificación`() {
        val request = CreateReviewRequest(calificacion = 5, comentario = "Entregó a tiempo")
        coEvery { api.createDistributorReview(4, request) } returns
            Response.success(DistributorReviewCreatedResponse("Reseña creada correctamente"))
        coEvery { api.getDistributorReviews(4, 1, 20) } returns Response.success(reviews)
        coEvery { api.getDistributorRating(4) } returns Response.success(rating)

        val viewModel = DistributorViewModel(api)
        viewModel.submitReview(distributorId = 4, calificacion = 5, comentario = "Entregó a tiempo")

        val state = viewModel.uiState.value
        assertTrue(state.reviewSubmitSuccess)
        assertEquals(4.5, state.rating?.calificacionPromedio ?: 0.0, 0.0)
        assertEquals(2, state.rating?.totalResenas)
        assertEquals(listOf(9), state.reviews.map { it.idResena })
        coVerify(exactly = 1) { api.createDistributorReview(4, request) }
    }

    @Test
    fun `no exige que el distribuidor tenga productos para poder reseñarlo`() {
        coEvery { api.createDistributorReview(7, any()) } returns
            Response.success(DistributorReviewCreatedResponse(null))
        coEvery { api.getDistributorReviews(7, 1, 20) } returns
            Response.success(reviews.copy(total = 0, reviews = emptyList()))
        coEvery { api.getDistributorRating(7) } returns Response.success(rating.copy(idDistribuidor = 7))

        val viewModel = DistributorViewModel(api)
        viewModel.submitReview(distributorId = 7, calificacion = 4, comentario = "")

        assertTrue(viewModel.uiState.value.reviewSubmitSuccess)
        assertEquals(null, viewModel.uiState.value.reviewSubmitError)
    }

    @Test
    fun `avisa cuando el agricultor ya calificó a ese distribuidor`() {
        coEvery { api.createDistributorReview(4, any()) } returns
            Response.error(409, "".toResponseBody())

        val viewModel = DistributorViewModel(api)
        viewModel.submitReview(distributorId = 4, calificacion = 3, comentario = "")

        assertEquals("Ya dejaste una reseña para este distribuidor", viewModel.uiState.value.reviewSubmitError)
    }
}
