package com.uvg.agroconecta.data.models

enum class UserRole(val apiValue: String) {
    FARMER("agricultor"),
    DISTRIBUTOR("distribuidor"),
    ADMIN("administrador");

    companion object {
        fun fromBackend(value: String?): UserRole? {
            val normalizedValue = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull {
                it.apiValue.equals(normalizedValue, ignoreCase = true)
            }
        }
    }
}
