package com.uvg.agroconecta.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.livedata.observeAsState
import com.uvg.agroconecta.ui.auth.AuthViewModel
import com.uvg.agroconecta.ui.auth.LoginScreen
import com.uvg.agroconecta.ui.auth.RegisterStep1Screen
import com.uvg.agroconecta.ui.auth.RegisterStep2Screen
import com.uvg.agroconecta.ui.home.HomeScreen
import com.uvg.agroconecta.ui.home.HomeViewModel
import com.uvg.agroconecta.ui.product.ProductDetailScreen
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.ui.cart.CartScreen
import com.uvg.agroconecta.ui.cart.CartViewModel
import com.uvg.agroconecta.ui.orders.OrderHistoryScreen
import com.uvg.agroconecta.ui.orders.OrderTrackingScreen
import com.uvg.agroconecta.ui.orders.OrderViewModel
import com.uvg.agroconecta.ui.publish.PublishProductScreen
import kotlinx.coroutines.flow.first
import com.uvg.agroconecta.ui.profile.ProfileScreen

@Composable
fun AgroConectaNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    initialTrackingOrderId: Int? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    val destination = initialTrackingOrderId?.let {
                        Screen.OrderTracking.createRoute(it)
                    } ?: Screen.Home.route

                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterStep1.route)
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.RegisterStep1.route) { backStackEntry ->
            val registerViewModel = backStackEntry.sharedAuthViewModel(navController)
            RegisterStep1Screen(
                viewModel = registerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNext = { navController.navigate(Screen.RegisterStep2.route) }
            )
        }

        composable(Screen.RegisterStep2.route) { backStackEntry ->
            val registerViewModel = backStackEntry.sharedAuthViewModel(navController)
            RegisterStep2Screen(
                viewModel = registerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            val nombre by authViewModel.nombreUsuario.observeAsState("")

            LaunchedEffect(nombre) {
                if (nombre.isNotBlank()) {
                    homeViewModel.setNombreAgricultor(nombre)
                }
            }

            HomeScreen(
                viewModel = homeViewModel,
                onProductoClick = { productoId ->
                    navController.navigate("product_detail/$productoId")
                },
                onVerMasProductos = { },
                onVerTodasCategorias = { },
                onCarritoClick = {
                    navController.navigate(Screen.Cart.route)
                },
                onPerfilClick = { navController.navigate(Screen.Profile.route) },
                onAgregarClick = { navController.navigate(Screen.PublishProduct.route) }
            )
        }

        composable(Screen.Cart.route) {
            val context = LocalContext.current
            val cartViewModel: CartViewModel = viewModel()
            val cartItems by cartViewModel.cartItems.collectAsState()
            val total by cartViewModel.total.collectAsState()
            val errorMessage by cartViewModel.errorMessage.collectAsState()

            LaunchedEffect(Unit) {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1

                if (farmerId != -1) {
                    cartViewModel.loadCart(idAgricultor = farmerId)
                }
            }

            CartScreen(
                items = cartItems,
                total = total,
                errorMessage = errorMessage,
                onIncreaseQuantity = { idItem ->
                    cartViewModel.increaseQuantity(idItem)
                },
                onDecreaseQuantity = { idItem ->
                    cartViewModel.decreaseQuantity(idItem)
                },
                onRemoveItem = { idItem ->
                    cartViewModel.removeItem(idItem)
                },
                onCheckout = { },
                onGoToCatalog = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(Screen.OrderHistory.route) {
            val context = LocalContext.current
            val orderViewModel: OrderViewModel = viewModel()
            val orders by orderViewModel.orders.collectAsState()
            val isLoading by orderViewModel.isLoading.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()

            LaunchedEffect(Unit) {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                val token = SessionManager.getToken(context).first()

                if (farmerId != -1) {
                    orderViewModel.loadOrdersByFarmer(farmerId, token)
                }
            }

            OrderHistoryScreen(
                orders = orders,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onTrackOrder = { orderId ->
                    navController.navigate(Screen.OrderTracking.createRoute(orderId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.OrderTracking.route,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val context = LocalContext.current
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            val orderViewModel: OrderViewModel = viewModel()
            val tracking by orderViewModel.tracking.collectAsState()
            val isLoading by orderViewModel.isLoading.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()
            var token by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(orderId) {
                token = SessionManager.getToken(context).first()
                orderViewModel.loadOrderTracking(orderId, token)
            }

            OrderTrackingScreen(
                tracking = tracking,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { navController.popBackStack() },
                onRetry = {
                    orderViewModel.loadOrderTracking(orderId, token)
                }
            )
        }

        composable(
            route = "product_detail/{productoId}",
            arguments = listOf(navArgument("productoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getInt("productoId") ?: return@composable
            ProductDetailScreen(
                productId = productoId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        }

        composable(Screen.PublishProduct.route) {
            PublishProductScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.resetLogin()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun NavBackStackEntry.sharedAuthViewModel(
    navController: NavHostController
): AuthViewModel {
    val parentEntry = remember(this) {
        navController.getBackStackEntry(Screen.RegisterStep1.route)
    }
    return viewModel(viewModelStoreOwner = parentEntry)
}
