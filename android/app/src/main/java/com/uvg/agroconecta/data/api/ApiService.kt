package com.uvg.agroconecta.data.api

import com.uvg.agroconecta.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Map<String, Any>>

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
    suspend fun getOrdersByFarmer(@Path("id") idAgricultor: Int): Response<List<Order>>
}
