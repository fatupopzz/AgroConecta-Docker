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
import com.uvg.agroconecta.ui.publish.PublishProductScreen
import kotlinx.coroutines.flow.first
import com.uvg.agroconecta.ui.profile.ProfileScreen
import com.uvg.agroconecta.ui.distributor.DistributorProfileScreen

@Composable
fun AgroConectaNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
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
            val context = LocalContext.current
            val homeViewModel: HomeViewModel = viewModel()
            val nombre by authViewModel.nombreUsuario.observeAsState("")

            LaunchedEffect(Unit) {
                val token = SessionManager.getToken(context).first()
                homeViewModel.init(token)
            }

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
                onAgregarClick = { navController.navigate(Screen.PublishProduct.route) },
                onDistribuidorClick = { distribuidorId ->        // ← nuevo
                    navController.navigate("distributor_profile/$distribuidorId")
                }

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
                val token = SessionManager.getToken(context).first() ?: return@LaunchedEffect
                if (farmerId != -1) {
                    cartViewModel.loadCart(idAgricultor = farmerId, token = token)
                }
            }

            CartScreen(
                items = cartItems,
                total = total,
                errorMessage = errorMessage,
                onIncreaseQuantity = { cartViewModel.increaseQuantity(it) },
                onDecreaseQuantity = { cartViewModel.decreaseQuantity(it) },
                onRemoveItem = { cartViewModel.removeItem(it) },
                onCheckout = { },
                onGoToCatalog = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onNavigateBack = { navController.popBackStack() }
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

        composable(
            route = "distributor_profile/{distribuidorId}",
            arguments = listOf(navArgument("distribuidorId") { type = NavType.IntType })
        ) { backStackEntry ->
            val distribuidorId = backStackEntry.arguments?.getInt("distribuidorId") ?: return@composable
            DistributorProfileScreen(
                distributorId = distribuidorId,
                onNavigateBack = { navController.popBackStack() },
                onProductoClick = { productoId ->
                    navController.navigate("product_detail/$productoId")
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


