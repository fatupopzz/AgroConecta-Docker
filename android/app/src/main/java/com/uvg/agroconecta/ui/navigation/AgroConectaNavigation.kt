package com.uvg.agroconecta.ui.navigation

sealed class Screen(val route: String) {

    data object Login : Screen("login")
    data object RegisterStep1 : Screen("register_step1")
    data object RegisterStep2 : Screen("register_step2")

    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object Cart : Screen("cart")
    data object Profile : Screen("profile")

    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: Int) = "product_detail/$productId"
    }

    data object Delivery : Screen(
        "delivery/{inventarioId}/{distribuidorId}/{productId}?productName={productName}&price={price}&distributorName={distributorName}"
    ) {
        fun createRoute(
            inventarioId: Int,
            distribuidorId: Int,
            productId: Int,
            productName: String,
            price: String,
            distributorName: String
        ) = "delivery/$inventarioId/$distribuidorId/$productId" +
                "?productName=${java.net.URLEncoder.encode(productName, "UTF-8")}" +
                "&price=${java.net.URLEncoder.encode(price, "UTF-8")}" +
                "&distributorName=${java.net.URLEncoder.encode(distributorName, "UTF-8")}"
    }
}