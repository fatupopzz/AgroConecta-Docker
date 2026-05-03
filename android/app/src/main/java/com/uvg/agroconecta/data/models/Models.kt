package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String
)

data class RegisterRequest(
    val nombre: String,
    val telefono: String,
    val email: String,
    val password: String
)

// ─── Product ─────────────────────────────────────────────────────────────────

data class Product(
    @SerializedName("id_producto") val id: Int,
    val nombre: String,
    val marca: String?,
    val descripcion: String?,
    val composicion: String?,
    @SerializedName("dosis_recomendada") val dosis: String?,
    @SerializedName("instrucciones_uso") val instrucciones: String?,
    @SerializedName("calificacion_promedio") val calificacion: Double,
    val categoria: String?,
    @SerializedName("precio_desde") val precioDesde: Double?,
    @SerializedName("num_distribuidores") val numDistribuidores: Int?,
    val activo: Boolean = true
)

data class ProductsResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val products: List<Product>
)

// ─── Product Detail (with distributor offers) ─────────────────────────────

data class ProductDetail(
    @SerializedName("id_producto") val id: Int,
    val nombre: String,
    val marca: String?,
    val descripcion: String?,
    val composicion: String?,
    @SerializedName("dosis_recomendada") val dosis: String?,
    @SerializedName("instrucciones_uso") val instrucciones: String?,
    @SerializedName("calificacion_promedio") val calificacion: Double,
    val categoria: String?,
    val ofertas: List<DistributorOffer>
)

data class DistributorOffer(
    @SerializedName("id_inventario") val idInventario: Int,
    val precio: Double,
    @SerializedName("stock_disponible") val stock: Int,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    @SerializedName("id_distribuidor") val idDistribuidor: Int,
    val distribuidor: String,               // nombre_negocio
    @SerializedName("calificacion_distribuidor") val calificacionDistribuidor: Double,
    // HU-023: sello verificado - viene del estado del distribuidor
    @SerializedName("estado_verificacion") val estadoVerificacion: String? = "verificado"
)

// ─── Comparison ──────────────────────────────────────────────────────────────

data class PriceComparison(
    @SerializedName("id_producto") val id: Int,
    val nombre: String,
    val marca: String?,
    @SerializedName("precio_mas_bajo") val precioMasBajo: Double?,
    val distribuidores: List<DistributorCompare>
)

data class DistributorCompare(
    @SerializedName("id_distribuidor") val idDistribuidor: Int,
    val nombre: String,
    val precio: Double,
    @SerializedName("stock_disponible") val stock: Int,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    @SerializedName("calificacion_distribuidor") val calificacion: Double,
    @SerializedName("es_precio_mas_bajo") val esPrecioMasBajo: Boolean
)

// ─── Category ────────────────────────────────────────────────────────────────

data class Category(
    @SerializedName("id_categoria") val id: Int,
    val nombre: String,
    val descripcion: String?
)

// ─── Distributor ─────────────────────────────────────────────────────────────

data class Distributor(
    @SerializedName("id_distribuidor") val id: Int,
    @SerializedName("nombre_negocio") val nombreNegocio: String,
    val departamento: String?,
    @SerializedName("estado_verificacion") val estadoVerificacion: String,
    @SerializedName("calificacion_promedio") val calificacion: Double,
    // joined from usuario
    val nombre: String?,
    val email: String?,
    val telefono: String?
)

// ─── Cart ────────────────────────────────────────────────────────────────────

data class CartResponse(
    @SerializedName("id_carrito") val idCarrito: Int?,
    val items: List<CartItem>,
    val total: Double
)

data class CartItem(
    @SerializedName("id_item") val idItem: Int,
    @SerializedName("id_inventario") val idInventario: Int,
    val cantidad: Int,
    @SerializedName("precio_unitario") val precioUnitario: Double,
    val subtotal: Double,
    val producto: String,
    val marca: String?,
    val distribuidor: String,
    @SerializedName("stock_disponible") val stock: Int,
    @SerializedName("unidad_medida") val unidadMedida: String?
)

data class AddItemRequest(
    @SerializedName("id_inventario") val idInventario: Int,
    val cantidad: Int
)

// ─── Order (HU-015 Entrega directa a la finca) ───────────────────────────────

data class CreateOrderRequest(
    @SerializedName("id_agricultor") val idAgricultor: Int,
    @SerializedName("id_distribuidor") val idDistribuidor: Int,
    @SerializedName("direccion_entrega") val direccionEntrega: String,
    val productos: List<OrderProduct>,
    @SerializedName("metodo_pago") val metodoPago: String = "contra_entrega"
)

data class OrderProduct(
    @SerializedName("id_inventario") val idInventario: Int,
    val cantidad: Int
)

data class OrderResponse(
    val message: String,
    val pedido: Order
)

data class Order(
    @SerializedName("id_pedido") val id: Int,
    @SerializedName("fecha_pedido") val fecha: String,
    val estado: String,
    @SerializedName("tipo_entrega") val tipoEntrega: String?,
    @SerializedName("direccion_entrega") val direccionEntrega: String,
    @SerializedName("total_pedido") val total: Double,
    @SerializedName("agricultor_nombre") val agricultorNombre: String?,
    @SerializedName("distribuidor_nombre") val distribuidorNombre: String?,
    @SerializedName("metodo_pago") val metodoPago: String?
)
