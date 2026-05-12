package com.uvg.agroconecta.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.uvg.agroconecta.ui.auth.LoginScreen
import com.uvg.agroconecta.ui.auth.RegisterScreen

@Composable
fun AgroConectaNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        // Limpia el stack para que back no regrese al login
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // TODO Fase 3: agregar destinos Home, ProductDetail, Delivery
        composable(Screen.Home.route) {
            // Placeholder temporal — lo migramos en la siguiente fase
            PlaceholderScreen(name = "Home")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text("Pantalla $name (migración pendiente)")
}