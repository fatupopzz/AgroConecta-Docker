package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.uvg.agroconecta.data.models.UserRole

enum class BottomNavTab { HOME, AGREGAR, PEDIDOS, PERFIL }

/**
 * Barra de navegación inferior. Lleva siempre la etiqueta debajo del ícono:
 * con poca experiencia digital un ícono solo es una adivinanza, y la segunda
 * pestaña además cambia según el tipo de cuenta.
 *
 * El ícono relleno marca la pestaña activa junto con el indicador, así no
 * depende solo del color.
 */
@Composable
fun AppBottomBar(
    selectedTab: BottomNavTab,
    tipoUsuario: String = "agricultor",
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    val role = UserRole.fromBackend(tipoUsuario)
    val canUseAddAction = role == UserRole.FARMER || role == UserRole.DISTRIBUTOR
    val addLabel = when (role) {
        UserRole.FARMER -> "Calcular"
        UserRole.DISTRIBUTOR -> "Publicar"
        UserRole.ADMIN, null -> "Agregar"
    }
    val addSelectedIcon = when (role) {
        UserRole.FARMER -> Icons.Filled.Calculate
        UserRole.DISTRIBUTOR, UserRole.ADMIN, null -> Icons.Filled.AddCircle
    }
    val addIdleIcon = when (role) {
        UserRole.FARMER -> Icons.Outlined.Calculate
        UserRole.DISTRIBUTOR, UserRole.ADMIN, null -> Icons.Outlined.AddCircleOutline
    }

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        BottomBarItem(
            selected = selectedTab == BottomNavTab.HOME,
            label = "Inicio",
            selectedIcon = Icons.Filled.Home,
            idleIcon = Icons.Outlined.Home,
            onClick = onHomeClick
        )
        BottomBarItem(
            selected = selectedTab == BottomNavTab.AGREGAR,
            label = addLabel,
            selectedIcon = addSelectedIcon,
            idleIcon = addIdleIcon,
            enabled = canUseAddAction,
            onClick = onAgregarClick
        )
        BottomBarItem(
            selected = selectedTab == BottomNavTab.PEDIDOS,
            label = "Pedidos",
            selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
            idleIcon = Icons.AutoMirrored.Filled.ReceiptLong,
            onClick = onPedidosClick
        )
        BottomBarItem(
            selected = selectedTab == BottomNavTab.PERFIL,
            label = "Perfil",
            selectedIcon = Icons.Filled.Person,
            idleIcon = Icons.Outlined.Person,
            onClick = onPerfilClick
        )
    }
}

@Composable
private fun RowScope.BottomBarItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    idleIcon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        enabled = enabled,
        // Tocar la pestaña en la que ya se está no debe recargar ni apilar nada.
        onClick = { if (enabled && !selected) onClick() },
        icon = {
            Icon(
                imageVector = if (selected) selectedIcon else idleIcon,
                contentDescription = null
            )
        },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
