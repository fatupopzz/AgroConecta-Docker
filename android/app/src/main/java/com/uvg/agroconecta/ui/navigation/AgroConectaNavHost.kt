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
import com.uvg.agroconecta.ui.notifications.DistributorNotificationViewModel
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.ui.product.ProductDetailScreen
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.ui.cart.CartScreen
import com.uvg.agroconecta.ui.cart.CartViewModel
import com.uvg.agroconecta.ui.distributor.DistributorProfileScreen
import com.uvg.agroconecta.ui.distributor.DistributorStatsScreen
import com.uvg.agroconecta.ui.dosecalculator.DoseCalculatorScreen
import com.uvg.agroconecta.ui.orders.OrderConfirmationScreen
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
            val token = SessionManager.getToken(context).first() ?: return@LaunchedEffect
            val tipo = SessionManager.getTipoUsuario(context).first() ?: return@LaunchedEffect
            if (tipo == "agricultor") {
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
            val notificationViewModel: DistributorNotificationViewModel = viewModel()
            val urgentNotification by notificationViewModel.urgentNotification.collectAsState()
            val nombre by authViewModel.nombreUsuario.observeAsState("")
            var sessionToken by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val token = SessionManager.getToken(context).first()
                sessionToken = token
                homeViewModel.init(token, tipoUsuario)
            }

            LaunchedEffect(tipoUsuario, sessionToken) {
                val token = sessionToken
                if (tipoUsuario == "distribuidor" && !token.isNullOrBlank()) {
                    notificationViewModel.loadUrgentNotification(token)
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
                    sessionToken?.let { token ->
                        notificationViewModel.markAsRead(notification, token)
                    }
                    navController.navigate(Screen.OrderHistory.route)
                },
                onProductoClick = { productoId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productoId))
                },
                onVerMasProductos = { },
                onVerTodasCategorias = { },
                onCarritoClick = { navController.navigate(Screen.Cart.route) },
                onPerfilClick = { navController.navigate(Screen.Profile.route) },
                onAgregarClick = onAgregarClick,
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
            val orderViewModel: OrderViewModel = viewModel()
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
                    val token = SessionManager.getToken(context).first()
                        ?: return@LaunchedEffect
                    if (farmerId != -1) {
                        sharedCartViewModel.loadCart(farmerId, token)
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
                            val token = SessionManager.getToken(context).first()
                                ?: return@launch
                            if (farmerId == -1) return@launch

                            orderViewModel.createCashOrder(
                                idAgricultor = farmerId,
                                items = listOf(product),
                                direccionEntrega = addressSnapshot,
                                tipoEntrega = "domicilio",
                                token = token,
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
            val orderViewModel: OrderViewModel = viewModel()
            val successMessage by orderViewModel.successMessage.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()
            val createdOrderId by orderViewModel.createdOrderId.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val cartItemsForOrder by sharedCartViewModel.cartItems.collectAsState()
            val total by sharedCartViewModel.total.collectAsState()

            var deliveryAddress by remember { mutableStateOf("") }
            var tipoEntrega by remember { mutableStateOf("domicilio") }

            var pickupAddress by remember {
                mutableStateOf<String?>(null)
            }

            var isLoadingPickupAddress by remember {
                mutableStateOf(false)
            }

            // ── KAN-60: pre-llenar dirección guardada ──
            LaunchedEffect(Unit) {
                val saved = SessionManager.getDeliveryAddress(context).first()
                if (!saved.isNullOrBlank()) deliveryAddress = saved
            }
                val distributorId = cartItemsForOrder
        .firstOrNull()
        ?.idDistribuidor

    LaunchedEffect(distributorId) {
        if (distributorId == null) {
            pickupAddress = null
            return@LaunchedEffect
        }

        isLoadingPickupAddress = true

        try {
            val token = SessionManager.getToken(context).first()
            val api = RetrofitClient.getService(token)

            val response = api.getDistributorById(distributorId)

            pickupAddress = if (response.isSuccessful) {
                response.body()?.direccion
            } else {
                null
            }
        } catch (e: Exception) {
            pickupAddress = null
        } finally {
            isLoadingPickupAddress = false
        }
}

            LaunchedEffect(successMessage) {
                successMessage?.let {
                    orderViewModel.clearSuccessMessage()

                    val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                    val token = SessionManager.getToken(context).first()
                    if (farmerId != -1) {
                        sharedCartViewModel.clearCart(idAgricultor = farmerId, token = token)
                    }

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
                        pickupAddress = pickupAddress,
                        isLoadingPickupAddress = isLoadingPickupAddress,
                        tipoEntrega = tipoEntrega,
                        onDeliveryAddressChange = { deliveryAddress = it },
                        onTipoEntregaChange = { tipoEntrega = it },
                        onConfirmOrder = {
                            scope.launch {
                                val farmerId =
                                    SessionManager.getFarmerId(context).first() ?: -1

                                val token =
                                    SessionManager.getToken(context).first()
                                        ?: return@launch

                                if (farmerId == -1) {
                                    return@launch
                                }

                                if (tipoEntrega == "domicilio") {
                                    SessionManager.saveDeliveryAddress(
                                        context,
                                        deliveryAddress
                                    )
                                }

                                orderViewModel.createCashOrder(
                                    idAgricultor = farmerId,
                                    items = cartItemsForOrder,
                                    direccionEntrega = if (tipoEntrega == "recogida") {
                                        pickupAddress.orEmpty()
                                    } else {
                                        deliveryAddress
        },
                                    tipoEntrega = tipoEntrega,
                                    token = token
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
            val orderViewModel: OrderViewModel = viewModel()
            val orders by orderViewModel.orders.collectAsState()
            val isLoading by orderViewModel.isLoading.collectAsState()
            val errorMessage by orderViewModel.errorMessage.collectAsState()

            LaunchedEffect(Unit) {
                val token = SessionManager.getToken(context).first()
                val role = SessionManager.getTipoUsuario(context).first() ?: "agricultor"
                if (role == "distribuidor") {
                    val distributorId = SessionManager.getPerfilId(context).first() ?: -1
                    if (distributorId != -1) {
                        orderViewModel.loadOrdersByDistributor(distributorId, token)
                    }
                } else {
                    val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                    if (farmerId != -1) {
                        orderViewModel.loadOrdersByFarmer(farmerId, token)
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
