package com.uvg.agroconecta.ui.navigation

import androidx.compose.runtime.*
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
import com.uvg.agroconecta.ui.cart.CartScreen
import com.uvg.agroconecta.ui.cart.CartViewModel

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
                onPerfilClick = { }
            )
        }

        composable(Screen.Cart.route) {
            val cartViewModel: CartViewModel = viewModel()
            val cartItems by cartViewModel.cartItems.collectAsState()
            val total by cartViewModel.total.collectAsState()

            LaunchedEffect(Unit) {
                cartViewModel.loadCart(idAgricultor = 1)
            }

            CartScreen(
                items = cartItems,
                total = total,
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
                    navController.navigate(Screen.Home.route)
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