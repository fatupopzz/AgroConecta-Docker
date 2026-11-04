package com.uvg.agroconecta.ui.navigation

import com.uvg.agroconecta.data.models.UserRole

internal data class RoleNavigationAccess(val role: UserRole?) {
    val isFarmer: Boolean = role == UserRole.FARMER
    val isDistributor: Boolean = role == UserRole.DISTRIBUTOR

    val addTabRoute: String = when (role) {
        UserRole.FARMER -> Screen.DoseCalculator.route
        UserRole.DISTRIBUTOR -> Screen.PublishProduct.route
        UserRole.ADMIN, null -> Screen.Home.route
    }

    companion object {
        fun fromBackend(value: String?): RoleNavigationAccess =
            RoleNavigationAccess(UserRole.fromBackend(value))
    }
}
