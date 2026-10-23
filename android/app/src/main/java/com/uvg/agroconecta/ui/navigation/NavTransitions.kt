package com.uvg.agroconecta.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/*
 * Transiciones entre pantallas. Son cortas a propósito (menos de 300 ms): en
 * teléfonos de gama baja una animación larga se siente como lentitud, no como
 * elegancia.
 *
 * - Entre pestañas de la barra inferior solo hay un fundido. Son pantallas
 *   hermanas y deslizarlas sugeriría que una está "dentro" de la otra.
 * - Al abrir una pantalla, entra desde la derecha con un desplazamiento
 *   corto. Atrás hace lo contrario, así se entiende hacia dónde se va.
 *
 * Compose respeta el ajuste de "escala de animación" del sistema, así que
 * quien desactiva las animaciones en el teléfono no las ve aquí tampoco.
 */

private val TAB_ROUTES = setOf(
    Screen.Home.route,
    Screen.OrderHistory.route,
    Screen.Profile.route,
    Screen.DoseCalculator.route,
    Screen.PublishProduct.route
)

internal fun isTabRoute(route: String?): Boolean = route in TAB_ROUTES

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    isTabRoute(initialState.destination.route) && isTabRoute(targetState.destination.route)

private const val ENTER_MS = 250
private const val EXIT_MS = 180

// Un octavo del ancho basta para que se note la dirección sin marear.
private fun slideOffset(fullWidth: Int) = fullWidth / 8

internal val agroEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (isTabSwitch()) {
        fadeIn(tween(ENTER_MS))
    } else {
        slideInHorizontally(tween(ENTER_MS, easing = FastOutSlowInEasing), ::slideOffset) +
            fadeIn(tween(ENTER_MS))
    }
}

internal val agroExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(EXIT_MS))
}

internal val agroPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(ENTER_MS))
}

internal val agroPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (isTabSwitch()) {
        fadeOut(tween(EXIT_MS))
    } else {
        slideOutHorizontally(tween(EXIT_MS, easing = FastOutSlowInEasing), ::slideOffset) +
            fadeOut(tween(EXIT_MS))
    }
}
