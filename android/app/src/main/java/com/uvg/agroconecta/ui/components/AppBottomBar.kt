package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VerdeAgroConecta = Color(0xFF2D6A1F)
private val VerdeClaro = Color(0xFF4CAF50)
private val GrisIcono = Color(0xFF757575)

enum class BottomNavTab { HOME, AGREGAR, PEDIDOS, PERFIL }

@Composable
fun AppBottomBar(
    selectedTab: BottomNavTab,
    tipoUsuario: String = "agricultor",
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {

        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HOME,
            onClick = onHomeClick,
            icon = {
                NavIcon(
                    selected = selectedTab == BottomNavTab.HOME,
                    icon = { tint -> Icon(Icons.Default.Home, contentDescription = "Home", tint = tint) }
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        if (tipoUsuario == "distribuidor") {
            NavigationBarItem(
                selected = selectedTab == BottomNavTab.AGREGAR,
                onClick = onAgregarClick,
                icon = {
                    NavIcon(
                        selected = selectedTab == BottomNavTab.AGREGAR,
                        icon = { tint -> Icon(Icons.Default.AddCircleOutline, contentDescription = "Publicar", tint = tint) }
                    )
                },
                label = null,
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }

        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PEDIDOS,
            onClick = onPedidosClick,
            icon = {
                NavIcon(
                    selected = selectedTab == BottomNavTab.PEDIDOS,
                    icon = { tint -> Icon(Icons.Default.List, contentDescription = "Pedidos", tint = tint) }
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PERFIL,
            onClick = onPerfilClick,
            icon = {
                NavIcon(
                    selected = selectedTab == BottomNavTab.PERFIL,
                    icon = { tint -> Icon(Icons.Default.Person, contentDescription = "Perfil", tint = tint) }
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}

@Composable
private fun NavIcon(
    selected: Boolean,
    icon: @Composable (tint: Color) -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (selected) VerdeClaro.copy(alpha = 0.2f) else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        icon(if (selected) VerdeAgroConecta else GrisIcono)
    }
}