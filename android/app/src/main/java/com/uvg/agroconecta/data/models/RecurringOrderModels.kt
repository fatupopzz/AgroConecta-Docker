package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class RecurringFrequency(val apiValue: String, val label: String) {
    DAILY("diaria", "Diaria"),
    WEEKLY("semanal", "Semanal"),
    BIWEEKLY("quincenal", "Quincenal"),
    MONTHLY("mensual", "Mensual");

    companion object {
        fun fromApi(value: String): RecurringFrequency =
            entries.firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Frecuencia desconocida: $value")
    }
}

enum class RecurringStatus(val apiValue: String, val label: String) {
    ACTIVE("activo", "Activo"),
    PAUSED("pausado", "Pausado"),
    CANCELED("cancelado", "Cancelado");

    companion object {
        fun fromApi(value: String): RecurringStatus =
            entries.firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Estado desconocido: $value")
    }
}

enum class RecurringAction(val apiValue: String) {
    PAUSE("pausar"), RESUME("reanudar"), CANCEL("cancelar")
}

data class RecurringProductDto(
    @SerializedName("id_inventario") val inventoryId: Int,
    @SerializedName("cantidad") val quantity: Int
)

data class RecurringOrderDto(
    val id: Int,
    @SerializedName("id_usuario") val userId: Int,
    @SerializedName("frecuencia") val frequency: String,
    @SerializedName("productos") val products: List<RecurringProductDto>,
    @SerializedName("id_distribuidor") val distributorId: Int,
    @SerializedName("direccion_entrega") val deliveryAddress: String,
    @SerializedName("metodo_pago") val paymentMethod: String,
    @SerializedName("fecha_proximo") val nextDate: String,
    @SerializedName("estado") val status: String
)

data class RecurringOrderResponse(
    val message: String,
    @SerializedName("pedido_recurrente") val order: RecurringOrderDto
)

data class CreateRecurringOrderRequest(
    @SerializedName("frecuencia") val frequency: String,
    @SerializedName("productos") val products: List<RecurringProductDto>,
    @SerializedName("id_distribuidor") val distributorId: Int,
    @SerializedName("direccion_entrega") val deliveryAddress: String,
    @SerializedName("metodo_pago") val paymentMethod: String = "contra_entrega",
    @SerializedName("fecha_proximo") val nextDate: String
)

data class UpdateRecurringOrderRequest(
    @SerializedName("frecuencia") val frequency: String? = null,
    @SerializedName("fecha_proximo") val nextDate: String? = null,
    @SerializedName("accion") val action: String? = null
)

data class OrderDetailDto(
    @SerializedName("id_pedido") val id: Int,
    @SerializedName("id_distribuidor") val distributorId: Int,
    @SerializedName("direccion_entrega") val deliveryAddress: String?,
    @SerializedName("productos") val products: List<OrderDetailProductDto>
)

data class OrderDetailProductDto(
    @SerializedName("id_inventario") val inventoryId: Int,
    @SerializedName("cantidad") val quantity: Int
)

data class RecurringOrder(
    val id: Int,
    val userId: Int,
    val frequency: RecurringFrequency,
    val products: List<RecurringProductDto>,
    val distributorId: Int,
    val deliveryAddress: String,
    val nextAt: Instant,
    val status: RecurringStatus
)

fun RecurringOrderDto.toDomain() = RecurringOrder(
    id = id,
    userId = userId,
    frequency = RecurringFrequency.fromApi(frequency),
    products = products,
    distributorId = distributorId,
    deliveryAddress = deliveryAddress,
    nextAt = Instant.parse(nextDate),
    status = RecurringStatus.fromApi(status)
)

object RecurringDates {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun display(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        formatter.format(instant.atZone(zone))

    fun parseLocal(value: String, zone: ZoneId = ZoneId.systemDefault()): Instant =
        LocalDateTime.parse(value.trim(), formatter).atZone(zone).toInstant()
}
