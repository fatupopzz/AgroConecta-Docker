package com.uvg.agroconecta.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import com.uvg.agroconecta.ui.auth.AuthViewModel
import com.uvg.agroconecta.ui.auth.LoginScreen
import com.uvg.agroconecta.ui.auth.RegisterStep1Screen
import com.uvg.agroconecta.ui.auth.RegisterStep2Screen
import com.uvg.agroconecta.ui.home.HomeScreen
import com.uvg.agroconecta.ui.home.HomeViewModel
import com.uvg.agroconecta.ui.home.CatalogScreen
import com.uvg.agroconecta.ui.help.HelpScreen
import com.uvg.agroconecta.ui.help.openWhatsAppSupport
import com.uvg.agroconecta.BuildConfig
import com.uvg.agroconecta.ui.notifications.DistributorNotificationViewModel
import com.uvg.agroconecta.ui.notifications.PriceNotificationViewModel
import com.uvg.agroconecta.ui.notifications.PriceNotificationState
import com.uvg.agroconecta.ui.notifications.priceNotificationProductId
import com.uvg.agroconecta.ui.product.ProductDetailScreen
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.ui.cart.CartScreen
import com.uvg.agroconecta.ui.cart.CartViewModel
import com.uvg.agroconecta.ui.components.LocalIsOffline
import com.uvg.agroconecta.ui.components.OfflineBanner
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.connectivity.ConnectivityViewModel
import com.uvg.agroconecta.ui.distributor.DistributorProfileScreen
import com.uvg.agroconecta.ui.distributor.DistributorStatsScreen
import com.uvg.agroconecta.ui.distributor.MyProductsScreen
import com.uvg.agroconecta.ui.dashboard.FarmerDashboardScreen
import com.uvg.agroconecta.ui.dosecalculator.DoseCalculatorScreen
import com.uvg.agroconecta.ui.favorites.FavoriteViewModel
import com.uvg.agroconecta.ui.favorites.FavoritesScreen
import com.uvg.agroconecta.ui.orders.OrderConfirmationScreen
import com.uvg.agroconecta.ui.orders.AdviceViewModel
import com.uvg.agroconecta.ui.orders.CheckoutViewModel
import com.uvg.agroconecta.ui.orders.OrderAdviceScreen
import com.uvg.agroconecta.ui.orders.OrderHistoryScreen
import com.uvg.agroconecta.ui.orders.OrderExportViewModel
import com.uvg.agroconecta.ui.orders.OrderExportState
import com.uvg.agroconecta.ui.orders.OrderPdfOpener
import com.uvg.agroconecta.ui.orders.OrderTrackingScreen
import com.uvg.agroconecta.ui.orders.OrderViewModel
import com.uvg.agroconecta.ui.orders.RecurringOrderViewModel
import com.uvg.agroconecta.ui.orders.RecurringOrderScreen
import com.uvg.agroconecta.ui.orders.RecurringOrderFormScreen
import com.uvg.agroconecta.ui.orders.UrgentOrderScreen
import com.uvg.agroconecta.ui.pestalerts.PestAlertRoute
import com.uvg.agroconecta.ui.publish.PublishProductScreen
import com.uvg.agroconecta.ui.profile.ProfileScreen
import com.uvg.agroconecta.ui.calendar.CropCalendarViewModel
import com.uvg.agroconecta.ui.calendar.CropCalendarScreen
import com.uvg.agroconecta.ui.home.WeatherViewModel
import com.uvg.agroconecta.ui.home.WeatherUiState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AgroConectaNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    initialTrackingOrderId: Int? = null,
    initialPestAlertId: Int? = null,
    onInitialPestAlertConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    // Se crea fuera de cualquier composable de destino, asi que el owner es la
    // MainActivity: una sola instancia compartida por todas las pantallas.
    val sharedCartViewModel: CartViewModel = hiltViewModel()
    val recurringViewModel: RecurringOrderViewModel = hiltViewModel()
    val cartItems by sharedCartViewModel.cartItems.collectAsState()
    val sharedFavoriteViewModel: FavoriteViewModel = hiltViewModel()
    val favoriteUiState by sharedFavoriteViewModel.uiState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    // Confirmar un pedido es el momento más importante de la compra; una
    // vibración corta lo confirma aunque la persona no esté mirando.
    val haptics = rememberAgroHaptics()

    // ── tipoUsuario a nivel global del NavHost ──
    val tipoUsuarioFlow by SessionManager.getTipoUsuario(context)
        .collectAsState(initial = null)
    val userIdFlow by SessionManager.getUserId(context)
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
        navController.navigateToTab(
            if (tipoUsuario == "distribuidor") Screen.PublishProduct.route else Screen.DoseCalculator.route
        )
    }
    val onHomeTabClick: () -> Unit = { navController.navigateToTab(Screen.Home.route) }
    val onPedidosTabClick: () -> Unit = { navController.navigateToTab(Screen.OrderHistory.route) }
    val onPerfilTabClick: () -> Unit = { navController.navigateToTab(Screen.Profile.route) }

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

    LaunchedEffect(tipoUsuarioFlow, userIdFlow) {
        val favoriteUserId = userIdFlow?.takeIf {
            tipoUsuarioFlow == "agricultor" && it > 0
        }
        sharedFavoriteViewModel.onUserChanged(favoriteUserId)
        recurringViewModel.onSessionChanged(favoriteUserId)
    }

    LaunchedEffect(initialPestAlertId, currentBackStackEntry?.destination?.route) {
        val currentRoute = currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        if (initialPestAlertId == null || currentRoute == Screen.Login.route) {
            return@LaunchedEffect
        }

        if (currentRoute != Screen.PestAlerts.route) {
            navController.navigate(Screen.PestAlerts.route) {
                launchSingleTop = true
            }
        }
    }

    // Aviso para el login cuando la cuenta se creó pero no se pudo entrar solo.
    var loginNotice by remember { mutableStateOf<String?>(null) }

    // Con una sesión vigente se entra directo a Inicio; null mientras se lee
    // DataStore, que tarda milisegundos, para no mostrar el login de paso.
    val startDestination by produceState<String?>(initialValue = null) {
        val token = SessionManager.getToken(context).first()
        val tipo = SessionManager.getTipoUsuario(context).first()
        value = if (hasUsableSession(token, tipo, System.currentTimeMillis() / 1000)) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }

    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val isOffline by connectivityViewModel.isOffline.collectAsState()

    // El aviso vive encima de todas las pantallas para que la persona sepa
    // por qué algo no carga, esté donde esté, sin que cada pantalla lo repita.
    val resolvedStart = startDestination ?: return

    // Con sesión recordada no se pasa por el login, que era donde se atendía
    // la notificación de un pedido; aquí se abre su seguimiento una sola vez.
    var trackingDeepLinkHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(resolvedStart, initialTrackingOrderId) {
        if (!trackingDeepLinkHandled && resolvedStart == Screen.Home.route && initialTrackingOrderId != null) {
            trackingDeepLinkHandled = true
            navController.navigate(Screen.OrderTracking.createRoute(initialTrackingOrderId))
        }
    }

    // Las pantallas de error leen la conexión desde aquí para reintentar solas
    // cuando vuelve la señal, sin que cada una tenga que pedirla.
    CompositionLocalProvider(LocalIsOffline provides isOffline) {
        Column(modifier = Modifier.fillMaxSize()) {
            OfflineBanner(isOffline = isOffline)
            NavHost(
                navController = navController,
                startDestination = resolvedStart,
                modifier = Modifier.weight(1f),
                enterTransition = agroEnterTransition,
                exitTransition = agroExitTransition,
                popEnterTransition = agroPopEnterTransition,
                popExitTransition = agroPopExitTransition
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            val destination = when {
                                initialPestAlertId != null -> Screen.PestAlerts.route
                                initialTrackingOrderId != null ->
                                    Screen.OrderTracking.createRoute(initialTrackingOrderId)
                                else -> Screen.Home.route
                            }
                            navController.navigate(destination) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            loginNotice = null
                            navController.navigate(Screen.RegisterStep1.route)
                        },
                        viewModel = authViewModel,
                        notice = loginNotice
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
                        onAccountCreated = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onAccountCreatedLoginFailed = {
                            loginNotice = "Tu cuenta quedó creada. Entra con tu correo y contraseña."
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val weatherViewModel: WeatherViewModel = hiltViewModel()
                    val weatherState by weatherViewModel.state.collectAsState()
                    val homeState by homeViewModel.uiState.collectAsState()
                    val lifecycleOwner = LocalLifecycleOwner.current
                    LaunchedEffect(userIdFlow, tipoUsuarioFlow, homeState.location) {
                        weatherViewModel.start(userIdFlow ?: 0, tipoUsuarioFlow.orEmpty(), homeState.location)
                    }
                    LaunchedEffect(lifecycleOwner, weatherViewModel) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            weatherViewModel.onForeground()
                            while (isActive) {
                                delay(60_000)
                                weatherViewModel.updateAge()
                            }
                        }
                    }
                    val notificationViewModel: DistributorNotificationViewModel = hiltViewModel()
                    val priceViewModel: PriceNotificationViewModel = hiltViewModel()
                    val priceState by priceViewModel.state.collectAsState()
                    LaunchedEffect(userIdFlow, tipoUsuarioFlow) {
                        priceViewModel.load(userIdFlow, tipoUsuarioFlow)
                    }
                    val urgentNotification by notificationViewModel.urgentNotification.collectAsState()
                    val nombreLogin by authViewModel.nombreUsuario.observeAsState("")
                    val nombreSesion by SessionManager.getUserName(context).collectAsState(initial = null)
                    val nombre = nombreLogin.ifBlank { nombreSesion.orEmpty() }

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
                        weatherState = if (weatherState.ownerId == userIdFlow) weatherState else WeatherUiState(),
                        onRefreshWeather = weatherViewModel::refresh,
                        onCropCalendarClick = { navController.navigate(Screen.CropCalendar.route) },
                        tipoUsuario = tipoUsuario,
                        cartItemCount = cartItems.size,
                        priceNotifications = if (priceState.ownerId == userIdFlow && tipoUsuarioFlow == "agricultor") priceState else PriceNotificationState(),
                        onPriceNotificationClick = { notification ->
                            priceNotificationProductId(notification)?.let { productId ->
                                navController.navigate(Screen.ProductDetail.createRoute(productId))
                            }
                        },
                        onPriceNotificationRead = priceViewModel::markAsRead,
                        onRetryPriceNotifications = { priceViewModel.load(userIdFlow, tipoUsuarioFlow) },
                        urgentNotification = urgentNotification,
                        onUrgentNotificationClick = { notification ->
                            notificationViewModel.markAsRead(notification)
                            navController.navigate(Screen.OrderHistory.route)
                        },
                        onRecommendedProductClick = { productName ->
                            navController.navigate(Screen.Catalog.createRoute(productName))
                        },
                        onPestAlertsClick = {
                            navController.navigate(Screen.PestAlerts.route) {
                                launchSingleTop = true
                            }
                        },
                        onProductoClick = { productoId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productoId))
                        },
                        onVerMasProductos = {
                            navController.navigate(Screen.Catalog.createRoute())
                        },
                        onSearch = { query ->
                            navController.navigate(Screen.Catalog.createRoute(query))
                        },
                        onCategoryClick = { categoryId ->
                            navController.navigate(Screen.Catalog.createRoute(category = categoryId))
                        },
                        onVerTodasCategorias = {
                            navController.navigate(Screen.Catalog.createRoute())
                        },
                        onCarritoClick = { navController.navigate(Screen.Cart.route) },
                        onPerfilClick = onPerfilTabClick,
                        onAgregarClick = onAgregarClick,
                        onPedidosClick = onPedidosTabClick,
                        onDistribuidorClick = { distribuidorId ->
                            navController.navigate(Screen.DistributorProfile.createRoute(distribuidorId))
                        },
                        favoriteIds = favoriteUiState.favoriteIds,
                        pendingFavoriteIds = favoriteUiState.pendingProductIds,
                        favoriteErrorMessage = favoriteUiState.errorMessage,
                        onFavoriteClick = sharedFavoriteViewModel::toggleFavorite,
                        onFavoriteErrorShown = sharedFavoriteViewModel::clearError
                    )
                }

                composable(Screen.CropCalendar.route) {
                    val calendarViewModel: CropCalendarViewModel = hiltViewModel()
                    LaunchedEffect(userIdFlow, tipoUsuarioFlow) {
                        calendarViewModel.start(userIdFlow ?: 0, tipoUsuarioFlow.orEmpty())
                    }
                    CropCalendarScreen(
                        calendarViewModel,
                        onBack = { navController.popBackStack() },
                        onCatalog = { category ->
                            navController.navigate(Screen.Catalog.createRoute(category = category ?: 0, calendar = true))
                        }
                    )
                }

                composable(
                    route = Screen.Catalog.route,
                    arguments = listOf(
                        navArgument("query") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("category") {
                            type = NavType.IntType
                            defaultValue = 0
                        },
                        navArgument("calendar") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val calendarContext = backStackEntry.arguments?.getBoolean("calendar") == true
                    CatalogScreen(
                        initialQuery = backStackEntry.arguments?.getString("query").orEmpty(),
                        initialCategoryId = backStackEntry.arguments?.getInt("category")?.takeIf { it > 0 },
                        calendarContext = calendarContext,
                        onBack = { navController.popBackStack() },
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId, chooseOffer = calendarContext))
                        },
                        showFavoriteAction = tipoUsuario == "agricultor",
                        favoriteIds = favoriteUiState.favoriteIds,
                        pendingFavoriteIds = favoriteUiState.pendingProductIds,
                        favoriteErrorMessage = favoriteUiState.errorMessage,
                        onFavoriteClick = sharedFavoriteViewModel::toggleFavorite,
                        onFavoriteErrorShown = sharedFavoriteViewModel::clearError
                    )
                }

                composable(Screen.Cart.route) {
                    val cartItemsState by sharedCartViewModel.cartItems.collectAsState()
                    val total by sharedCartViewModel.total.collectAsState()
                    val errorMessage by sharedCartViewModel.errorMessage.collectAsState()
                    val isLoadingCart by sharedCartViewModel.isLoading.collectAsState()

                    val cartScope = rememberCoroutineScope()
                    val loadCart: () -> Unit = {
                        cartScope.launch {
                            val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                            if (farmerId != -1) {
                                sharedCartViewModel.loadCart(idAgricultor = farmerId)
                            }
                        }
                    }

                    LaunchedEffect(Unit) { loadCart() }

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
                            navController.navigate(Screen.Catalog.createRoute())
                        },
                        isLoading = isLoadingCart,
                        onRefresh = loadCart,
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
                        haptics.confirm()
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
                    val cartItemsForOrder by sharedCartViewModel.cartItems.collectAsState()
                    val total by sharedCartViewModel.total.collectAsState()
                    val farmerId by SessionManager.getFarmerId(context).collectAsState(initial = null)

                    LaunchedEffect(Unit) {
                        checkoutViewModel.setInitialDeliveryAddress(
                            SessionManager.getDeliveryAddress(context).first()
                        )
                    }
                    LaunchedEffect(cartItemsForOrder) {
                        checkoutViewModel.onCartItemsChange(cartItemsForOrder)
                    }
                    val distributorId = cartItemsForOrder.firstOrNull()?.idDistribuidor
                    LaunchedEffect(distributorId) {
                        checkoutViewModel.loadPickupAddress(distributorId)
                    }
                    LaunchedEffect(checkoutState.createdOrderId) {
                        if (checkoutState.createdOrderId != null) haptics.confirm()
                        checkoutViewModel.completeOrder(
                            clearCart = sharedCartViewModel::clearCart,
                            navigate = { orderId ->
                                navController.navigate(Screen.OrderTracking.createRoute(orderId)) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    OrderConfirmationScreen(
                        items = cartItemsForOrder,
                        total = total,
                        deliveryAddress = checkoutState.deliveryAddress,
                        pickupAddress = checkoutState.pickupAddress,
                        isLoadingPickupAddress = checkoutState.isLoadingPickupAddress,
                        tipoEntrega = checkoutState.deliveryType,
                        isCreatingOrder = checkoutState.isCreatingOrder,
                        errorMessage = checkoutState.errorMessage,
                        canSubmit = checkoutState.canConfirm && (farmerId ?: -1) > 0,
                        canRetry = checkoutState.canRetry,
                        onRetryOrder = checkoutViewModel::retryOrder,
                        onDeliveryAddressChange = checkoutViewModel::onDeliveryAddressChange,
                        onTipoEntregaChange = checkoutViewModel::onDeliveryTypeChange,
                        onConfirmOrder = {
                            farmerId?.takeIf { it > 0 }?.let { id ->
                                // Acquire the ViewModel guard synchronously on the first tap.
                                checkoutViewModel.createCashOrder(id, cartItemsForOrder)
                                if (checkoutState.deliveryType == "domicilio") {
                                    scope.launch {
                                        SessionManager.saveDeliveryAddress(context, checkoutState.deliveryAddress)
                                    }
                                }
                            }
                        },
                        onBack = {
                            checkoutViewModel.clearError()
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.OrderHistory.route) {
                    val orderViewModel: OrderViewModel = hiltViewModel()
                    val exportViewModel: OrderExportViewModel = hiltViewModel()
                    val exportState by exportViewModel.state.collectAsState()
                    var exportNotice by remember { mutableStateOf<String?>(null) }
                    val orders by orderViewModel.orders.collectAsState()
                    val isLoading by orderViewModel.isLoading.collectAsState()
                    val errorMessage by orderViewModel.errorMessage.collectAsState()

                    LaunchedEffect(exportState) {
                        val completed = exportState as? OrderExportState.Completed
                        if (completed != null) {
                            exportNotice = if (OrderPdfOpener.open(context, completed.uri)) {
                                "PDF descargado y abierto."
                            } else {
                                "PDF descargado. Instala un lector de PDF para abrirlo."
                            }
                            exportViewModel.clearResult()
                        }
                    }

                    val orderScope = rememberCoroutineScope()
                    val loadOrders: () -> Unit = {
                        orderScope.launch {
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
                    }

                    LaunchedEffect(Unit) { loadOrders() }

                    OrderHistoryScreen(
                        orders = orders,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        tipoUsuario = tipoUsuarioFlow ?: "",
                        exportState = exportState,
                        exportNotice = exportNotice,
                        onExportPdf = {
                            exportNotice = null
                            exportViewModel.export()
                        },
                        onTrackOrder = { orderId ->
                            navController.navigate(Screen.OrderTracking.createRoute(orderId))
                        },
                        onOpenAdvice = { orderId ->
                            navController.navigate(Screen.OrderAdvice.createRoute(orderId))
                        },
                        onBack = { navController.popBackStack() },
                        onHomeClick = onHomeTabClick,
                        onAgregarClick = onAgregarClick,
                        onPerfilClick = onPerfilTabClick,
                        onRecurringOrders = { navController.navigate(Screen.RecurringOrders.route) },
                        onMakeRecurring = { id -> navController.navigate(Screen.RecurringForm.createRoute("order", id)) },
                        onRetry = loadOrders
                    )
                }

                composable(Screen.RecurringOrders.route) {
                    val state by recurringViewModel.list.collectAsState()
                    LaunchedEffect(currentBackStackEntry?.destination?.route, userIdFlow, tipoUsuarioFlow) {
                        if (currentBackStackEntry?.destination?.route == Screen.RecurringOrders.route &&
                            tipoUsuarioFlow == "agricultor" && (userIdFlow ?: -1) > 0
                        ) recurringViewModel.load(userIdFlow!!)
                    }
                    if (tipoUsuarioFlow == "agricultor") {
                        RecurringOrderScreen(
                            state = state,
                            onRetry = { userIdFlow?.let(recurringViewModel::load) },
                            onCreateNew = { navController.navigate(Screen.RecurringForm.createRoute("cart")) },
                            onEdit = { id -> navController.navigate(Screen.RecurringForm.createRoute("edit", id)) },
                            onAction = recurringViewModel::action,
                            onPermissionGranted = recurringViewModel::onNotificationPermissionGranted,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(
                    Screen.RecurringForm.route,
                    arguments = listOf(
                        navArgument("source") { type = NavType.StringType },
                        navArgument("id") { type = NavType.IntType }
                    )
                ) { entry ->
                    val source = entry.arguments?.getString("source") ?: return@composable
                    val id = entry.arguments?.getInt("id") ?: 0
                    val state by recurringViewModel.form.collectAsState()
                    val submitting by recurringViewModel.submitting.collectAsState()
                    LaunchedEffect(source, id, userIdFlow, tipoUsuarioFlow) {
                        if (tipoUsuarioFlow == "agricultor" && (userIdFlow ?: -1) > 0) {
                            val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                            recurringViewModel.prepare(source, id, userIdFlow!!, farmerId)
                        }
                    }
                    LaunchedEffect(state.complete) {
                        if (state.complete) {
                            recurringViewModel.clearFormCompletion()
                            if (source == "order") {
                                navController.navigate(Screen.RecurringOrders.route) {
                                    popUpTo(Screen.OrderHistory.route) { inclusive = false }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                    if (tipoUsuarioFlow == "agricultor") {
                        RecurringOrderFormScreen(
                            state = state,
                            submitting = submitting,
                            onFrequency = recurringViewModel::setFrequency,
                            onAddress = recurringViewModel::setAddress,
                            onNextDate = recurringViewModel::setNextDate,
                            onSubmit = recurringViewModel::submit,
                            onBack = { navController.popBackStack() }
                        )
                    }
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
                    arguments = listOf(
                        navArgument("productoId") { type = NavType.IntType },
                        navArgument("chooseOffer") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val productoId = backStackEntry.arguments?.getInt("productoId") ?: return@composable
                    val scope = rememberCoroutineScope()
                    ProductDetailScreen(
                        productId = productoId,
                        requireOfferSelection = backStackEntry.arguments?.getBoolean("chooseOffer") == true,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                        onAddedToCart = {
                            scope.launch {
                                val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                                if (farmerId != -1) {
                                    sharedCartViewModel.loadCart(idAgricultor = farmerId)
                                }
                            }
                        },
                        isFavorite = productoId in favoriteUiState.favoriteIds,
                        isUpdatingFavorite = productoId in favoriteUiState.pendingProductIds,
                        favoriteErrorMessage = favoriteUiState.errorMessage,
                        onFavoriteClick = { sharedFavoriteViewModel.toggleFavorite(productoId) },
                        onFavoriteErrorShown = sharedFavoriteViewModel::clearError
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
                        },
                        showFavoriteAction = tipoUsuario == "agricultor",
                        favoriteIds = favoriteUiState.favoriteIds,
                        pendingFavoriteIds = favoriteUiState.pendingProductIds,
                        favoriteErrorMessage = favoriteUiState.errorMessage,
                        onFavoriteClick = sharedFavoriteViewModel::toggleFavorite,
                        onFavoriteErrorShown = sharedFavoriteViewModel::clearError
                    )
                }

                composable(Screen.PublishProduct.route) {
                    PublishProductScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onHomeClick = onHomeTabClick,
                        onPedidosClick = onPedidosTabClick,
                        onPerfilClick = onPerfilTabClick,
                        tipoUsuario = tipoUsuario
                    )
                }

                composable(Screen.DoseCalculator.route) {
                    DoseCalculatorScreen(
                        onHomeClick = onHomeTabClick,
                        onPedidosClick = onPedidosTabClick,
                        onPerfilClick = onPerfilTabClick
                    )
                }

                composable(Screen.PestAlerts.route) {
                    PestAlertRoute(
                        onNavigateBack = { navController.popBackStack() },
                        initialAlertId = initialPestAlertId,
                        onInitialAlertHandled = onInitialPestAlertConsumed,
                        showFavoriteAction = tipoUsuario == "agricultor",
                        favoriteIds = favoriteUiState.favoriteIds,
                        pendingFavoriteIds = favoriteUiState.pendingProductIds,
                        favoriteErrorMessage = favoriteUiState.errorMessage,
                        onFavoriteClick = sharedFavoriteViewModel::toggleFavorite,
                        onFavoriteErrorShown = sharedFavoriteViewModel::clearError
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onHomeClick = onHomeTabClick,
                        onAgregarClick = onAgregarClick,
                        onPedidosClick = onPedidosTabClick,
                        onFavoritesClick = {
                            navController.navigate(Screen.Favorites.route) {
                                launchSingleTop = true
                            }
                        },
                        onDashboardClick = {
                            navController.navigate(Screen.FarmerDashboard.route) {
                                launchSingleTop = true
                            }
                        },
                        onHelpClick = {
                            navController.navigate(Screen.Help.route) {
                                launchSingleTop = true
                            }
                        },
                        onStatsClick = {
                            navController.navigate(Screen.DistributorStats.route) {
                                launchSingleTop = true
                            }
                        },
                        onMyProductsClick = {
                            navController.navigate(Screen.MyProducts.route) {
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

                composable(Screen.Favorites.route) {
                    LaunchedEffect(Unit) {
                        sharedFavoriteViewModel.loadFavorites()
                    }
                    FavoritesScreen(
                        uiState = favoriteUiState,
                        onNavigateBack = { navController.popBackStack() },
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        },
                        onToggleFavorite = sharedFavoriteViewModel::toggleFavorite,
                        onRetry = sharedFavoriteViewModel::loadFavorites,
                        onErrorShown = sharedFavoriteViewModel::clearError
                    )
                }

                composable(Screen.Help.route) {
                    HelpScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onContactSupport = {
                            val opened = openWhatsAppSupport(
                                context = context,
                                phoneNumber = BuildConfig.SUPPORT_WHATSAPP_NUMBER
                            )
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    "No se pudo abrir WhatsApp. Intenta nuevamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                composable(Screen.DistributorStats.route) {
                    DistributorStatsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.MyProducts.route) {
                    MyProductsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.FarmerDashboard.route) {
                    FarmerDashboardScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * Navegación entre pestañas de la barra inferior. Sin esto cada toque apilaba
 * otra copia de la pantalla y el botón atrás paseaba por todas las pestañas
 * visitadas; ahora atrás desde cualquier pestaña regresa a Inicio y cada una
 * conserva su scroll al volver.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
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
