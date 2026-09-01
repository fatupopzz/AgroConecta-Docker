package com.uvg.agroconecta.data.api

import com.uvg.agroconecta.data.models.*
import retrofit2.Response
import retrofit2.http.*
import com.uvg.agroconecta.ui.profile.FarmerProfile
import com.uvg.agroconecta.ui.profile.DistributorProfile


interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Map<String, Any>>

    @GET("auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<MeResponse>

    @GET("ciclos/{cultivo}")
    suspend fun getCropCycles(
        @Path("cultivo") cultivo: String
    ): Response<CropCycleResponse>

    // ── Products ─────────────────────────────────────────────────────────
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("nombre") nombre: String? = null,
        @Query("id_categoria") idCategoria: Int? = null,
        @Header("Authorization") token: String? = null
    ): Response<ProductsResponse>

    // Recomendados y distribuidores llevan el token como @Header porque
    // HomeViewModel ya consume el ApiService inyectado por Hilt, que no tiene
    // interceptor de auth.
    @GET("productos/recomendados")
    suspend fun getRecommendedProducts(
        @Header("Authorization") token: String?
    ): Response<List<Product>>

    @GET("products/{id}")
    suspend fun getProductById(
        @Path("id") id: Int,
        @Header("Authorization") token: String? = null
    ): Response<ProductDetail>

    @GET("products/{id}/compare")
    suspend fun compareProductPrices(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Response<PriceComparison>

    // El seguimiento de precios no existe sin sesion (el backend contesta
    // 401/403), asi que el token va sin default: que el llamador lo pase siempre.
    @GET("products/{id}/seguidos")
    suspend fun getProductFollowStatus(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Response<ProductFollowResponse>

    @POST("products/{id}/seguir")
    suspend fun followProductPrice(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Response<ProductFollowResponse>

    @DELETE("products/{id}/seguir")
    suspend fun unfollowProductPrice(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Response<ProductFollowResponse>

    // ── Categories ───────────────────────────────────────────────────────
    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    // ── Distributors ─────────────────────────────────────────────────────
    @GET("distribuidores")
    suspend fun getVerifiedDistributors(
        @Header("Authorization") token: String? = null
    ): Response<List<Distributor>>

    // ── Cart ─────────────────────────────────────────────────────────────
    // Estos endpoints reciben el token como @Header porque CartViewModel usa el
    // ApiService inyectado por Hilt, que no lleva interceptor de auth.
    @GET("cart/{id_agricultor}")
    suspend fun getCart(
        @Path("id_agricultor") idAgricultor: Int,
        @Header("Authorization") token: String?
    ): Response<CartResponse>

    @POST("cart/{id_agricultor}/items")
    suspend fun addToCart(
        @Path("id_agricultor") idAgricultor: Int,
        @Header("Authorization") token: String? = null,
        @Body request: AddItemRequest
    ): Response<Map<String, Any>>

    @PATCH("cart/{id_agricultor}/items/{id_item}")
    suspend fun updateCartItem(
        @Path("id_agricultor") idAgricultor: Int,
        @Path("id_item") idItem: Int,
        @Header("Authorization") token: String?,
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    @DELETE("cart/{id_agricultor}/items/{id_item}")
    suspend fun removeCartItem(
        @Path("id_agricultor") idAgricultor: Int,
        @Path("id_item") idItem: Int,
        @Header("Authorization") token: String?
    ): Response<Map<String, Any>>

    @DELETE("cart/{id_agricultor}")
    suspend fun clearCart(
        @Path("id_agricultor") idAgricultor: Int,
        @Header("Authorization") token: String?
    ): Response<Map<String, Any>>

    // ── Orders (HU-015) ──────────────────────────────────────────────────
    @POST("orders")
    suspend fun createOrder(
        @Header("Authorization") token: String?,
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @GET("orders/farmer/{id}")
    suspend fun getOrdersByFarmer(
        @Path("id") idAgricultor: Int,
        @Header("Authorization") token: String?,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("estado") estado: String? = null
    ): Response<OrdersByFarmerResponse>

    @GET("orders/distributor/{id}")
    suspend fun getOrdersByDistributor(
        @Path("id") idDistribuidor: Int,
        @Header("Authorization") token: String?
    ): Response<List<OrderSummary>>

    @GET("orders/{id}/tracking")
    suspend fun getOrderTracking(
        @Path("id") orderId: Int,
        @Header("Authorization") token: String?
    ): Response<OrderTrackingResponse>

    // ── Order technical advice (HU-033) ──────────────────────────────────
    @GET("orders/{id}/advice")
    suspend fun getAdviceMessages(
        @Path("id") orderId: Int
    ): Response<AdviceMessagesResponse>

    @POST("orders/{id}/advice")
    suspend fun sendAdviceMessage(
        @Path("id") orderId: Int,
        @Body request: SendAdviceMessageRequest
    ): Response<AdviceMessage>

    // ── Distributor notifications ─────────────────────────────────────────
    @GET("notifications")
    suspend fun getDistributorNotifications(
        @Header("Authorization") token: String?
    ): Response<List<DistributorNotification>>

    @PATCH("notifications/{id}/read")
    suspend fun markDistributorNotificationAsRead(
        @Path("id") notificationId: Int,
        @Header("Authorization") token: String?
    ): Response<Map<String, String>>

    @GET("products/{id}/reviews")
    suspend fun getReviews(
        @Path("id")    productoId: Int,
        @Header("Authorization") token: String?
    ): Response<ReviewsResponse>

    @POST("products/{id}/reviews")
    suspend fun createReview(
        @Path("id")              productoId: Int,
        @Header("Authorization") token: String,
        @Body                    body: CreateReviewRequest
    ): Response<Review>

    // ── Inventory ─────────────────────────────────────────────────────────────
    @POST("inventory")
    suspend fun createInventory(
        @Header("Authorization") token: String,
        @Body request: CreateInventoryRequest
    ): Response<Map<String, Any>>

    // ── Products (distribuidor) ───────────────────────────────────────────────
    @POST("products")
    suspend fun createProduct(
        @Header("Authorization") token: String,
        @Body request: CreateProductRequest
    ): Response<CreateProductResponse>


    @GET("farmers/profile/{id}")
    suspend fun getFarmerProfile(
        @Header("Authorization") token: String,
        @Path("id") farmerId: Int
    ): Response<FarmerProfile>


    @GET("distribuidores/{id}/rating")
    suspend fun getDistributorRating(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Response<DistributorRatingResponse>

    @GET("distribuidores/{id}/reviews")
    suspend fun getDistributorReviews(
        @Path("id") id: Int,
        @Header("Authorization") token: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<DistributorReviewsResponse>

    @GET("distribuidores/{id}/stats")
    suspend fun getDistributorStats(
        @Path("id") id: Int,
        @Header("Authorization") token: String?
    ): Response<DistributorStatsResponse>

    // ── Distributor by ID ─────────────────────────────────────────────────
    @GET("distribuidores/{id}")
    suspend fun getDistributorById(
        @Path("id") id: Int,
        @Header("Authorization") token: String? = null
    ): Response<DistributorProfile>


}

