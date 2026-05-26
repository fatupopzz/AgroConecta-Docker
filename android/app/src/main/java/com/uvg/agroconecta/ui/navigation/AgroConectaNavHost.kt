package com.uvg.agroconecta.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.cart.CartViewModel
import com.uvg.agroconecta.ui.distributor.DistributorProfileScreen
import com.uvg.agroconecta.ui.orders.OrderConfirmationScreen
import com.uvg.agroconecta.ui.orders.OrderHistoryScreen
import com.uvg.agroconecta.ui.orders.OrderTrackingScreen
import com.uvg.agroconecta.ui.orders.OrderViewModel
import com.uvg.agroconecta.ui.publish.PublishProductScreen
import com.uvg.agroconecta.ui.profile.ProfileScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AgroConectaNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    initialTrackingOrderId: Int? = null
) {
    val context = LocalContext.current
    val sharedCartViewModel: CartViewModel = viewModel()
    val cartItems by sharedCartViewModel.cartItems.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        val route = currentBackStackEntry?.destination?.route
        if (route == Screen.Home.route) {
            val token = SessionManager.getToken(context).first() ?: return@LaunchedEffect
            val tipoUsuario = SessionManager.getTipoUsuario(context).first() ?: return@LaunchedEffect
            if (tipoUsuario == "agricultor") {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                if (farmerId != -1) {
                    sharedCartViewModel.loadCart(idAgricultor = farmerId, token = token)
                }
            }
        }
    }

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
            var tipoUsuario by remember { mutableStateOf("agricultor") }

            LaunchedEffect(Unit) {
                val token = SessionManager.getToken(context).first()
                tipoUsuario = SessionManager.getTipoUsuario(context).first() ?: "agricultor"
                homeViewModel.init(token)
            }

            LaunchedEffect(nombre) {
                if (nombre.isNotBlank()) homeViewModel.setNombreAgricultor(nombre)
            }

            HomeScreen(
                viewModel = homeViewModel,
                tipoUsuario = tipoUsuario,
                cartItemCount = cartItems.size,
                onProductoClick = { productoId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productoId))
                },
                onVerMasProductos = { },
                onVerTodasCategorias = { },
                onCarritoClick = { navController.navigate(Screen.Cart.route) },
                onPerfilClick = { navController.navigate(Screen.Profile.route) },
                onAgregarClick = { navController.navigate(Screen.PublishProduct.route) },
                onPedidosClick = { navController.navigate(Screen.OrderHistory.route) },
                onDistribuidorClick = { distribuidorId ->
                    navController.navigate(Screen.DistributorProfile.createRoute(distribuidorId))
                }
            )
        }

        composable(Screen.Cart.route) {
            val cartItemsState by sharedCartViewModel.cartItems.collectAsState()
            val total by sharedCartViewModel.total.collectAsState()
            val errorMessage by sharedCartViewModel.errorMessage.collectAsState()

            LaunchedEffect(Unit) {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                val token = SessionManager.getToken(context).first() ?: return@LaunchedEffect
                if (farmerId != -1) {
                    sharedCartViewModel.loadCart(idAgricultor = farmerId, token = token)
                }
            }

            CartScreen(
                items = cartItemsState,
                total = total,
                errorMessage = errorMessage,
                onIncreaseQuantity = { sharedCartViewModel.increaseQuantity(it) },
                onDecreaseQuantity = { sharedCartViewModel.decreaseQuantity(it) },
                onRemoveItem = { sharedCartViewModel.removeItem(it) },
                onCheckout = {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("cart_items", ArrayList(cartItemsState))
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("cart_total", total)
                    navController.navigate(Screen.OrderConfirmation.route)
                },
                onGoToCatalog = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrderConfirmation.route) {
            val scope = rememberCoroutineScope()
            val orderViewModel: OrderViewModel = viewModel()
            val successMessage by orderViewModel.successMessage.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()
            val createdOrderId by orderViewModel.createdOrderId.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            val cartItemsForOrder: List<CartItemUI> by sharedCartViewModel.cartItems.observeAsState(emptyList())
            val total: Double by sharedCartViewModel.total.observeAsState(0.0)

            var deliveryAddress by remember { mutableStateOf("") }

            LaunchedEffect(successMessage) {
                successMessage?.let {
                    snackbarHostState.showSnackbar("¡Pedido creado exitosamente!")
                    orderViewModel.clearSuccessMessage()

                    // Limpiar carrito en backend y frontend
                    val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                    val token = SessionManager.getToken(context).first()
                    if (farmerId != -1) {
                        sharedCartViewModel.clearCart(idAgricultor = farmerId, token = token)
                    }

                    // Navegar a tracking del pedido recién creado
                    val orderId = createdOrderId
                    orderViewModel.clearCreatedOrderId()
                    if (orderId != null) {
                        navController.navigate(Screen.OrderTracking.createRoute(orderId)) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Screen.OrderHistory.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }
                }
            }

            LaunchedEffect(errorMessage) {
                errorMessage?.let { snackbarHostState.showSnackbar(it) }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    OrderConfirmationScreen(
                        items = cartItemsForOrder,
                        total = total,
                        selectedPaymentMethod = "efectivo",
                        deliveryAddress = deliveryAddress,
                        onDeliveryAddressChange = { deliveryAddress = it },
                        onConfirmOrder = {
                            scope.launch {
                                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                                val token = SessionManager.getToken(context).first() ?: return@launch
                                if (farmerId == -1) return@launch
                                orderViewModel.createCashOrder(
                                    idAgricultor = farmerId,
                                    items = cartItemsForOrder,
                                    direccionEntrega = deliveryAddress,
                                    token = token
                                )
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(Screen.OrderHistory.route) {
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
                onBack = { navController.popBackStack() },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAgregarClick = { navController.navigate(Screen.PublishProduct.route) },
                onPerfilClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.OrderTracking.route,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
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
                onRetry = { orderViewModel.loadOrderTracking(orderId, token) }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productoId = backStackEntry.arguments?.getInt("productoId") ?: return@composable
            val scope = rememberCoroutineScope()
            ProductDetailScreen(
                productId = productoId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                onAddedToCart = {
                    scope.launch {
                        val token = SessionManager.getToken(context).first() ?: return@launch
                        val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                        if (farmerId != -1) {
                            sharedCartViewModel.loadCart(idAgricultor = farmerId, token = token)
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.DistributorProfile.route,
            arguments = listOf(navArgument("distribuidorId") { type = NavType.IntType })
        ) { backStackEntry ->
            val distribuidorId = backStackEntry.arguments?.getInt("distribuidorId") ?: return@composable
            DistributorProfileScreen(
                distributorId = distribuidorId,
                onNavigateBack = { navController.popBackStack() },
                onProductoClick = { productoId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productoId))
                }
            )
        }

        composable(Screen.PublishProduct.route) {
            PublishProductScreen(
                onNavigateBack = { navController.popBackStack() },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPedidosClick = { navController.navigate(Screen.OrderHistory.route) },
                onPerfilClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAgregarClick = { navController.navigate(Screen.PublishProduct.route) },
                onPedidosClick = { navController.navigate(Screen.OrderHistory.route) },
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