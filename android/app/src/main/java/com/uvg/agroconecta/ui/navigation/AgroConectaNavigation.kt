package com.uvg.agroconecta.ui.navigation

sealed class Screen(val route: String) {

    data object Login : Screen("login")
    data object RegisterStep1 : Screen("register_step1")
    data object RegisterStep2 : Screen("register_step2")

    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object Cart : Screen("cart")
    data object PaymentMethod : Screen("payment_method")
    data object OrderConfirmation : Screen("order_confirmation")
    data object OrderHistory : Screen("order_history")
    data object Profile : Screen("profile")
    data object PublishProduct : Screen("publish_product")

    data object ProductDetail : Screen("product_detail/{productoId}") {
        fun createRoute(productoId: Int) = "product_detail/$productoId"
    }

    data object DistributorProfile : Screen("distributor_profile/{distribuidorId}") {
        fun createRoute(distribuidorId: Int) = "distributor_profile/$distribuidorId"
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
                "?productName=${android.net.Uri.encode(productName)}" +
                "&price=${android.net.Uri.encode(price)}" +
                "&distributorName=${android.net.Uri.encode(distributorName)}"
    }
}