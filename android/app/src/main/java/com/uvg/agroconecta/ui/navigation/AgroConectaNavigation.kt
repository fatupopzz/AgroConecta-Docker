package com.uvg.agroconecta.ui.navigation

/**
 * Rutas de navegación de la app, tipadas como strings con argumentos.
 * Si en el futuro queremos type-safe nav con kotlinx-serialization, migramos aquí.
 */
sealed class Screen(val route: String) {

    // Auth flow
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Main flow (después de login)
    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object Cart : Screen("cart")
    data object Profile : Screen("profile")

    // Detalle de producto con argumento productId
    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: Int) = "product_detail/$productId"
    }

    // Entrega — HU-015
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