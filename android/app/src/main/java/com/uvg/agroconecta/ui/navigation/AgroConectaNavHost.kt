package com.uvg.agroconecta.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.uvg.agroconecta.ui.home.CatalogScreen
import com.uvg.agroconecta.ui.notifications.DistributorNotificationViewModel
import com.uvg.agroconecta.ui.product.ProductDetailScreen
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.ui.cart.CartScreen
import com.uvg.agroconecta.ui.cart.CartViewModel
import com.uvg.agroconecta.ui.distributor.DistributorProfileScreen
import com.uvg.agroconecta.ui.distributor.DistributorStatsScreen
import com.uvg.agroconecta.ui.dosecalculator.DoseCalculatorScreen
import com.uvg.agroconecta.ui.orders.OrderConfirmationScreen
import com.uvg.agroconecta.ui.orders.AdviceViewModel
import com.uvg.agroconecta.ui.orders.CheckoutViewModel
import com.uvg.agroconecta.ui.orders.OrderAdviceScreen
import com.uvg.agroconecta.ui.orders.OrderHistoryScreen
import com.uvg.agroconecta.ui.orders.OrderTrackingScreen
import com.uvg.agroconecta.ui.orders.OrderViewModel
import com.uvg.agroconecta.ui.orders.UrgentOrderScreen
import com.uvg.agroconecta.ui.publish.PublishProductScreen
import com.uvg.agroconecta.ui.profile.ProfileScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AgroConectaNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    initialTrackingOrderId: Int? = null
) {
    val context = LocalContext.current
    // Se crea fuera de cualquier composable de destino, asi que el owner es la
    // MainActivity: una sola instancia compartida por todas las pantallas.
    val sharedCartViewModel: CartViewModel = hiltViewModel()
    val cartItems by sharedCartViewModel.cartItems.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // ── tipoUsuario a nivel global del NavHost ──
    val tipoUsuarioFlow by SessionManager.getTipoUsuario(context)
        .collectAsState(initial = null)
    val tipoUsuario = tipoUsuarioFlow ?: "agricultor"

    LaunchedEffect(Unit) {
        SessionManager.sessionExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // ── Lambda compartida: onAgregarClick ──
    val onAgregarClick: () -> Unit = {
        if (tipoUsuario == "distribuidor") {
            navController.navigate(Screen.PublishProduct.route) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(Screen.DoseCalculator.route) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        val route = currentBackStackEntry?.destination?.route
        if (route == Screen.Home.route) {
            val tipo = SessionManager.getTipoUsuario(context).first() ?: return@LaunchedEffect
            if (tipo == "agricultor") {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                if (farmerId != -1) {
                    sharedCartViewModel.loadCart(idAgricultor = farmerId)
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
            val homeViewModel: HomeViewModel = hiltViewModel()
            val notificationViewModel: DistributorNotificationViewModel = hiltViewModel()
            val urgentNotification by notificationViewModel.urgentNotification.collectAsState()
            val nombre by authViewModel.nombreUsuario.observeAsState("")

            LaunchedEffect(Unit) {
                val sessionUserType = SessionManager.getTipoUsuario(context).first() ?: tipoUsuario
                homeViewModel.init(sessionUserType)
            }

            LaunchedEffect(tipoUsuario) {
                if (tipoUsuario == "distribuidor") {
                    notificationViewModel.loadUrgentNotification()
                }
            }

            LaunchedEffect(nombre) {
                if (nombre.isNotBlank()) homeViewModel.setNombreAgricultor(nombre)
            }

            HomeScreen(
                viewModel = homeViewModel,
                tipoUsuario = tipoUsuario,
                cartItemCount = cartItems.size,
                urgentNotification = urgentNotification,
                onUrgentNotificationClick = { notification ->
                    notificationViewModel.markAsRead(notification)
                    navController.navigate(Screen.OrderHistory.route)
                },
                onRecommendedProductClick = { productName ->
                    navController.navigate(Screen.Catalog.createRoute(productName))
                },
                onProductoClick = { productoId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productoId))
                },
                onVerMasProductos = {
                    navController.navigate(Screen.Catalog.createRoute())
                },
                onVerTodasCategorias = {
                    navController.navigate(Screen.Catalog.createRoute())
                },
                onCarritoClick = { navController.navigate(Screen.Cart.route) },
                onPerfilClick = { navController.navigate(Screen.Profile.route) },
                onAgregarClick = onAgregarClick,
                onPedidosClick = { navController.navigate(Screen.OrderHistory.route) },
                onDistribuidorClick = { distribuidorId ->
                    navController.navigate(Screen.DistributorProfile.createRoute(distribuidorId))
                }
            )
        }

        composable(
            route = Screen.Catalog.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            CatalogScreen(
                initialQuery = backStackEntry.arguments?.getString("query").orEmpty(),
                onBack = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            )
        }

        composable(Screen.Cart.route) {
            val cartItemsState by sharedCartViewModel.cartItems.collectAsState()
            val total by sharedCartViewModel.total.collectAsState()
            val errorMessage by sharedCartViewModel.errorMessage.collectAsState()

            LaunchedEffect(Unit) {
                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                if (farmerId != -1) {
                    sharedCartViewModel.loadCart(idAgricultor = farmerId)
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
                    navController.navigate(Screen.OrderConfirmation.route)
                },
                onUrgentOrder = {
                    navController.navigate(Screen.UrgentOrder.route)
                },
                onGoToCatalog = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.UrgentOrder.route) {
            val scope = rememberCoroutineScope()
            val orderViewModel: OrderViewModel = hiltViewModel()
            val cartItemsForUrgency by sharedCartViewModel.cartItems.collectAsState()
            val isCreatingOrder by orderViewModel.isLoading.collectAsState()
            val successMessage by orderViewModel.successMessage.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()
            val createdOrderId by orderViewModel.createdOrderId.collectAsState()

            var deliveryAddress by remember { mutableStateOf("") }
            var submittedCartItemId by remember { mutableStateOf<Int?>(null) }
            var submittedDeliveryAddress by remember { mutableStateOf<String?>(null) }
            var isPreparingSubmission by remember { mutableStateOf(false) }
            val isSubmitting = isPreparingSubmission || isCreatingOrder

            LaunchedEffect(Unit) {
                deliveryAddress = SessionManager.getDeliveryAddress(context).first().orEmpty()

                if (cartItemsForUrgency.isEmpty()) {
                    val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                    if (farmerId != -1) {
                        sharedCartViewModel.loadCart(farmerId)
                    }
                }
            }

            LaunchedEffect(successMessage) {
                if (successMessage == null) return@LaunchedEffect

                orderViewModel.clearSuccessMessage()
                submittedCartItemId?.let(sharedCartViewModel::removeItem)
                submittedDeliveryAddress?.let { address ->
                    SessionManager.saveDeliveryAddress(context, address)
                }

                val orderId = createdOrderId
                orderViewModel.clearCreatedOrderId()
                if (orderId != null) {
                    navController.navigate(Screen.OrderTracking.createRoute(orderId)) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.OrderHistory.route) {
                        popUpTo(Screen.Cart.route) { inclusive = true }
                    }
                }
            }

            UrgentOrderScreen(
                cartItems = cartItemsForUrgency,
                deliveryAddress = deliveryAddress,
                isSubmitting = isSubmitting,
                errorMessage = errorMessage,
                onDeliveryAddressChange = { deliveryAddress = it },
                onConfirmUrgentOrder = onConfirm@{ product, pestType ->
                    if (isSubmitting) return@onConfirm

                    val addressSnapshot = deliveryAddress.trim()
                    val pestSnapshot = pestType.trim()
                    isPreparingSubmission = true
                    submittedCartItemId = product.id
                    submittedDeliveryAddress = addressSnapshot
                    scope.launch {
                        try {
                            val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                            if (farmerId == -1) return@launch

                            orderViewModel.createCashOrder(
                                idAgricultor = farmerId,
                                items = listOf(product),
                                direccionEntrega = addressSnapshot,
                                tipoEntrega = "domicilio",
                                esUrgente = true,
                                tipoPlaga = pestSnapshot
                            )
                        } finally {
                            isPreparingSubmission = false
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrderConfirmation.route) {
            val scope = rememberCoroutineScope()
            val checkoutViewModel: CheckoutViewModel = hiltViewModel()
            val checkoutState by checkoutViewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val cartItemsForOrder by sharedCartViewModel.cartItems.collectAsState()
            val total by sharedCartViewModel.total.collectAsState()

            // ── KAN-60: pre-llenar dirección guardada ──
            LaunchedEffect(Unit) {
                val saved = SessionManager.getDeliveryAddress(context).first()
                checkoutViewModel.setInitialDeliveryAddress(saved)
            }

            val distributorId = cartItemsForOrder.firstOrNull()?.idDistribuidor

            LaunchedEffect(distributorId) {
                checkoutViewModel.loadPickupAddress(distributorId)
            }

            LaunchedEffect(checkoutState.successMessage) {
                checkoutState.successMessage?.let {
                    checkoutViewModel.clearSuccessMessage()

                    val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                    if (farmerId != -1) {
                        sharedCartViewModel.clearCart(idAgricultor = farmerId)
                    }

                    val orderId = checkoutState.createdOrderId
                    checkoutViewModel.clearCreatedOrderId()
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

            LaunchedEffect(checkoutState.errorMessage) {
                checkoutState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    OrderConfirmationScreen(
                        items = cartItemsForOrder,
                        total = total,
                        deliveryAddress = checkoutState.deliveryAddress,
                        pickupAddress = checkoutState.pickupAddress,
                        isLoadingPickupAddress = checkoutState.isLoadingPickupAddress,
                        tipoEntrega = checkoutState.deliveryType,
                        onDeliveryAddressChange = checkoutViewModel::onDeliveryAddressChange,
                        onTipoEntregaChange = checkoutViewModel::onDeliveryTypeChange,
                        onConfirmOrder = {
                            scope.launch {
                                val farmerId =
                                    SessionManager.getFarmerId(context).first() ?: -1

                                if (farmerId == -1) {
                                    return@launch
                                }

                                if (checkoutState.deliveryType == "domicilio") {
                                    SessionManager.saveDeliveryAddress(
                                        context,
                                        checkoutState.deliveryAddress
                                    )
                                }

                                checkoutViewModel.createCashOrder(
                                    idAgricultor = farmerId,
                                    items = cartItemsForOrder
                                )
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        composable(Screen.OrderHistory.route) {
            val orderViewModel: OrderViewModel = hiltViewModel()
            val orders by orderViewModel.orders.collectAsState()
            val isLoading by orderViewModel.isLoading.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()

            LaunchedEffect(Unit) {
                val role = SessionManager.getTipoUsuario(context).first() ?: "agricultor"
                if (role == "distribuidor") {
                    val distributorId = SessionManager.getPerfilId(context).first() ?: -1
                    if (distributorId != -1) {
                        orderViewModel.loadOrdersByDistributor(distributorId)
                    }
                } else {
                    val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                    if (farmerId != -1) {
                        orderViewModel.loadOrdersByFarmer(farmerId)
                    }
                }
            }

            OrderHistoryScreen(
                orders = orders,
                isLoading = isLoading,
                errorMessage = errorMessage,
                tipoUsuario = tipoUsuario,
                onTrackOrder = { orderId ->
                    navController.navigate(Screen.OrderTracking.createRoute(orderId))
                },
                onOpenAdvice = { orderId ->
                    navController.navigate(Screen.OrderAdvice.createRoute(orderId))
                },
                onBack = { navController.popBackStack() },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onAgregarClick = onAgregarClick,
                onPerfilClick = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.OrderTracking.route,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            val orderViewModel: OrderViewModel = hiltViewModel()
            val tracking by orderViewModel.tracking.collectAsState()
            val isLoading by orderViewModel.isLoading.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()

            LaunchedEffect(orderId) {
                orderViewModel.loadOrderTracking(orderId)
            }

            OrderTrackingScreen(
                tracking = tracking,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { navController.popBackStack() },
                onRetry = { orderViewModel.loadOrderTracking(orderId) },
                onOpenAdvice = { id ->
                    navController.navigate(Screen.OrderAdvice.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.OrderAdvice.route,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            val adviceViewModel: AdviceViewModel = hiltViewModel()
            val messages by adviceViewModel.messages.collectAsState()
            val isLoading by adviceViewModel.isLoading.collectAsState()
            val isSending by adviceViewModel.isSending.collectAsState()
            val errorMessage by adviceViewModel.errorMessage.collectAsState()
            var currentUserId by remember { mutableIntStateOf(-1) }

            LaunchedEffect(orderId) {
                currentUserId = SessionManager.getUserId(context).first() ?: -1
                adviceViewModel.startPolling(orderId)
            }

            OrderAdviceScreen(
                orderId = orderId,
                currentUserId = currentUserId,
                messages = messages,
                isLoading = isLoading,
                isSending = isSending,
                errorMessage = errorMessage,
                onSendMessage = { message ->
                    adviceViewModel.sendMessage(orderId, message)
                },
                onRetry = { adviceViewModel.retry(orderId) },
                onDismissError = adviceViewModel::clearError,
                onBack = { navController.popBackStack() }
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
                        val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                        if (farmerId != -1) {
                            sharedCartViewModel.loadCart(idAgricultor = farmerId)
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
                onPerfilClick = { navController.navigate(Screen.Profile.route) },
                tipoUsuario = tipoUsuario
            )
        }

        composable(Screen.DoseCalculator.route) {
            DoseCalculatorScreen(
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
                onAgregarClick = onAgregarClick,
                onPedidosClick = { navController.navigate(Screen.OrderHistory.route) },
                onStatsClick = {
                    navController.navigate(Screen.DistributorStats.route) {
                        launchSingleTop = true
                    }
                },
                tipoUsuario = tipoUsuario,
                onLogout = {
                    authViewModel.resetLogin()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DistributorStats.route) {
            DistributorStatsScreen(
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
    return hiltViewModel(viewModelStoreOwner = parentEntry)
}
