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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (selectedTab == BottomNavTab.HOME)
                                VerdeClaro.copy(alpha = 0.2f)
                            else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (selectedTab == BottomNavTab.HOME) VerdeAgroConecta
                        else GrisIcono
                    )
                }
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.AGREGAR,
            onClick = onAgregarClick,
            icon = {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = "Agregar",
                    tint = if (selectedTab == BottomNavTab.AGREGAR) VerdeAgroConecta
                    else GrisIcono
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PEDIDOS,
            onClick = onPedidosClick,
            icon = {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Pedidos",
                    tint = if (selectedTab == BottomNavTab.PEDIDOS) VerdeAgroConecta
                    else GrisIcono
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.PERFIL,
            onClick = onPerfilClick,
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = if (selectedTab == BottomNavTab.PERFIL) VerdeAgroConecta
                    else GrisIcono
                )
            },
            label = null,
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}