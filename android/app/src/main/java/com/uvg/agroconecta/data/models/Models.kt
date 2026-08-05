package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val nombre: String? = null,
    @SerializedName("tipoUsuario") val tipoUsuario: String? = null,
    @SerializedName("idPerfil")    val idPerfil: Int? = null
)
data class RegisterRequest(
    val nombre: String,
    val apellido: String?,
    val telefono: String,
    val email: String,
    val password: String,
    @SerializedName("tipo_usuario") val tipoUsuario: String,
    val departamento: String?,
    val municipio: String?,
    @SerializedName("nombre_negocio") val nombreNegocio: String?,
    val nit: String?
)

data class MeResponse(
    val user: UserInfo,
    val perfil: PerfilInfo?
)

data class UserInfo(
    @SerializedName("id_usuario") val idUsuario: Int,
    val nombre: String?,
    val email: String?,
    val telefono: String?,
    @SerializedName("tipo_usuario") val tipoUsuario: String?
)

data class PerfilInfo(
    @SerializedName("id_agricultor") val idAgricultor: Int? = null,
    @SerializedName("id_distribuidor") val idDistribuidor: Int? = null,
    @SerializedName("nombre_negocio") val nombreNegocio: String? = null,
    @SerializedName("estado_verificacion") val estadoVerificacion: String? = null,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double? = null
)

enum class TipoCuenta(val apiValue: String, val displayName: String) {
    AGRICULTOR("agricultor", "Agricultor"),
    DISTRIBUIDOR("distribuidor", "Distribuidor")
}

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
    @SerializedName("estado_verificacion") val estadoVerificacion: String?
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

data class ProductFollowResponse(
    val siguiendo: Boolean,
    @SerializedName("producto_seguido") val productoSeguido: FollowedProduct? = null,
    val message: String? = null
)

data class FollowedProduct(
    val id: Int,
    @SerializedName("id_agricultor") val idAgricultor: Int,
    @SerializedName("id_producto") val idProducto: Int,
    @SerializedName("precio_al_seguir") val precioAlSeguir: Double,
    val fecha: String?
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


data class DistributorRatingResponse(
    @SerializedName("id_distribuidor") val idDistribuidor: Int,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double,
    @SerializedName("total_resenas") val totalResenas: Int,
    val distribucion: RatingDistribution
)

data class RatingDistribution(
    @SerializedName("5") val cinco: Int,
    @SerializedName("4") val cuatro: Int,
    @SerializedName("3") val tres: Int,
    @SerializedName("2") val dos: Int,
    @SerializedName("1") val una: Int
)

data class DistributorReviewsResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    @SerializedName("total_pages") val totalPages: Int,
    val reviews: List<DistributorReview>
)

data class DistributorReview(
    @SerializedName("id_resena") val idResena: Int,
    val calificacion: Int,
    val comentario: String?,
    @SerializedName("fecha_resena") val fechaResena: String?,
    @SerializedName("id_producto") val idProducto: Int,
    @SerializedName("producto_nombre") val productoNombre: String?,
    @SerializedName("agricultor_nombre") val agricultorNombre: String?
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
    @SerializedName("id_distribuidor") val idDistribuidor: Int,
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
    @SerializedName("tipo_entrega") val tipoEntrega: String,
    val productos: List<OrderProduct>,
    @SerializedName("metodo_pago") val metodoPago: String = "contra_entrega",
    val esUrgente: Boolean = false,
    val tipoPlaga: String? = null
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
    @SerializedName("es_urgente") val esUrgente: Boolean = false,
    @SerializedName("tipo_plaga") val tipoPlaga: String? = null,
    @SerializedName("total_pedido") val total: Double,
    @SerializedName("agricultor_nombre") val agricultorNombre: String?,
    @SerializedName("distribuidor_nombre") val distribuidorNombre: String?,
    @SerializedName("metodo_pago") val metodoPago: String?
)

// ─── Orders by Farmer (paginated) ──────────────────────────────────────

data class OrdersByFarmerResponse(
    val data: List<OrderSummary>,
    val total: Int,
    val page: Int,
    @SerializedName("totalPages") val totalPages: Int
)

data class OrderSummary(
    val id: Int,
    val estado: String,
    @SerializedName("fecha_pedido") val fechaPedido: String,
    @SerializedName("total_pedido") val totalPedido: Double,
    @SerializedName("distribuidor_nombre") val distribuidorNombre: String? = null,
    @SerializedName("agricultor_nombre") val agricultorNombre: String? = null,
    @SerializedName("cantidad_productos") val cantidadProductos: Int,
    @SerializedName("es_urgente") val esUrgente: Boolean = false,
    @SerializedName("tipo_plaga") val tipoPlaga: String? = null
)

// ─── Order Tracking ─────────────────────────────────────────────────────────

data class OrderTrackingResponse(
    @SerializedName("id_pedido") val idPedido: Int,
    @SerializedName("estado_actual") val estadoActual: String,
    val cambios: List<OrderTrackingChange>,
    @SerializedName("tiempo_estimado_entrega") val tiempoEstimadoEntrega: String?
)

data class OrderTrackingChange(
    val estado: String,
    val timestamp: String,
    val notas: String?
)

// ─── Distributor notifications ───────────────────────────────────────────────

data class DistributorNotification(
    @SerializedName("id_notificacion") val id: Int,
    val tipo: String,
    val contenido: DistributorNotificationContent,
    @SerializedName("id_pedido") val idPedido: Int? = null,
    val leida: Boolean = false,
    val fecha: String? = null
)

data class DistributorNotificationContent(
    val mensaje: String? = null,
    val agricultor: String? = null,
    val monto: Double? = null,
    val pedido: Int? = null,
    val esUrgente: Boolean = false,
    val tipoPlaga: String? = null
)

// ─── Publish Product (KAN-53) ─────────────────────────────────────────────

data class CreateProductRequest(
    val nombre: String,
    val marca: String?,
    val descripcion: String?,
    @SerializedName("id_categoria") val idCategoria: Int,
    val composicion: String?,
    @SerializedName("dosis_recomendada") val dosis: String?,
    @SerializedName("instrucciones_uso") val instrucciones: String?
)

data class CreateProductResponse(
    val message: String,
    @SerializedName("producto") val producto: ProductoCreado
)

data class ProductoCreado(
    @SerializedName("id_producto") val idProducto: Int,
    val nombre: String
)

data class CreateInventoryRequest(
    @SerializedName("id_producto") val idProducto: Int,
    val precio: Double,
    @SerializedName("stock_disponible") val stock: Int,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    @SerializedName("tiempo_entrega_dias") val tiempoEntrega: Int?
)
