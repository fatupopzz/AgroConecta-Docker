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

    // ── Products ─────────────────────────────────────────────────────────
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("nombre") nombre: String? = null,
        @Query("id_categoria") idCategoria: Int? = null
    ): Response<ProductsResponse>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: Int): Response<ProductDetail>

    @GET("products/{id}/compare")
    suspend fun compareProductPrices(@Path("id") id: Int): Response<PriceComparison>

    // ── Categories ───────────────────────────────────────────────────────
    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    // ── Distributors ─────────────────────────────────────────────────────
    @GET("distribuidores")
    suspend fun getVerifiedDistributors(): Response<List<Distributor>>

    // ── Cart ─────────────────────────────────────────────────────────────
    @GET("cart/{id_agricultor}")
    suspend fun getCart(@Path("id_agricultor") idAgricultor: Int): Response<CartResponse>

    @POST("cart/{id_agricultor}/items")
    suspend fun addToCart(
        @Path("id_agricultor") idAgricultor: Int,
        @Body request: AddItemRequest
    ): Response<Map<String, Any>>

    @PATCH("cart/{id_agricultor}/items/{id_item}")
    suspend fun updateCartItem(
        @Path("id_agricultor") idAgricultor: Int,
        @Path("id_item") idItem: Int,
        @Body body: Map<String, Int>
    ): Response<Map<String, Any>>

    @DELETE("cart/{id_agricultor}/items/{id_item}")
    suspend fun removeCartItem(
        @Path("id_agricultor") idAgricultor: Int,
        @Path("id_item") idItem: Int
    ): Response<Map<String, Any>>

    // ── Orders (HU-015) ──────────────────────────────────────────────────
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderResponse>

    @GET("orders/farmer/{id}")
    suspend fun getOrdersByFarmer(
        @Path("id") idAgricultor: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("estado") estado: String? = null
    ): Response<OrdersByFarmerResponse>

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
        @Path("id") id: Int
    ): Response<DistributorRatingResponse>

    @GET("distribuidores/{id}/reviews")
    suspend fun getDistributorReviews(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<DistributorReviewsResponse>

    // ── Distributor by ID ─────────────────────────────────────────────────
    @GET("distribuidores/{id}")
    suspend fun getDistributorById(
        @Path("id") id: Int
    ): Response<DistributorProfile>


}

