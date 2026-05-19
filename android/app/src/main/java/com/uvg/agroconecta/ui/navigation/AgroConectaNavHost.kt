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
import com.uvg.agroconecta.ui.orders.OrderConfirmationScreen
import com.uvg.agroconecta.ui.orders.OrderHistoryScreen
import com.uvg.agroconecta.ui.orders.OrderViewModel
import com.uvg.agroconecta.ui.payment.PaymentMethodScreen
import com.uvg.agroconecta.ui.publish.PublishProductScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AgroConectaNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }
    var deliveryAddress by remember { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
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
                onPerfilClick = { },
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
                onCheckout = {
                    navController.navigate(Screen.PaymentMethod.route)
                },
                onGoToCatalog = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
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

        composable(Screen.PaymentMethod.route) {
            PaymentMethodScreen(
                selectedMethod = selectedPaymentMethod,
                onMethodSelected = { method ->
                    selectedPaymentMethod = method
                },
                onContinue = {
                    navController.navigate(Screen.OrderConfirmation.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.OrderConfirmation.route) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val cartViewModel: CartViewModel = viewModel()
            val orderViewModel: OrderViewModel = viewModel()
            val cartItems by cartViewModel.cartItems.collectAsState()
            val total by cartViewModel.total.collectAsState()
            val successMessage by orderViewModel.successMessage.collectAsState()

            LaunchedEffect(Unit) {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1

                if (farmerId != -1) {
                    cartViewModel.loadCart(idAgricultor = farmerId)
                }
            }

            LaunchedEffect(successMessage) {
                if (successMessage != null) {
                    deliveryAddress = ""
                    selectedPaymentMethod = null
                    navController.navigate(Screen.OrderHistory.route) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                }
            }

            OrderConfirmationScreen(
                items = cartItems,
                total = total,
                selectedPaymentMethod = selectedPaymentMethod ?: "efectivo",
                deliveryAddress = deliveryAddress,
                onDeliveryAddressChange = { address ->
                    deliveryAddress = address
                },
                onConfirmOrder = {
                    val currentPaymentMethod = selectedPaymentMethod ?: "efectivo"

                    if (currentPaymentMethod == "efectivo") {
                        val farmerIdFlow = SessionManager.getFarmerId(context)

                        scope.launch {
                            val farmerId = farmerIdFlow.first() ?: -1

                            if (farmerId != -1) {
                                orderViewModel.createCashOrder(
                                    idAgricultor = farmerId,
                                    items = cartItems,
                                    direccionEntrega = deliveryAddress
                                )
                            }
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
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

                if (farmerId != -1) {
                    orderViewModel.loadOrdersByFarmer(farmerId)
                }
            }

            OrderHistoryScreen(
                orders = orders,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.PublishProduct.route) {
            PublishProductScreen(
                onNavigateBack = { navController.popBackStack() }
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
