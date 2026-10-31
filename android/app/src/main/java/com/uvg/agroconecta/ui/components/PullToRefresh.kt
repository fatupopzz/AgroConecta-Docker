package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Deslizar hacia abajo para recargar, con el indicador de Material 3.
 *
 * Solo muestra su indicador cuando la persona hace el gesto. Las cargas que
 * arrancan solas ya tienen siluetas o barra de progreso, y dos indicadores
 * a la vez confunden.
 *
 * [isRefreshing] es el estado de carga del ViewModel. El indicador espera a
 * que esa carga empiece y termine. Si no empieza (por ejemplo, porque el
 * ViewModel ignora una carga repetida), se retira solo y no queda girando.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgroPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState(enabled = { enabled })
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val refreshing by rememberUpdatedState(isRefreshing)

    if (state.isRefreshing) {
        LaunchedEffect(Unit) {
            currentOnRefresh()
            withTimeoutOrNull(START_TIMEOUT_MS) { snapshotFlow { refreshing }.first { it } }
            snapshotFlow { refreshing }.first { !it }
            state.endRefresh()
        }
    }

    // El recorte evita que la sombra del indicador asome bajo la barra superior
    // cuando está escondido, un detalle de la versión 1.2 del componente.
    Box(
        modifier = modifier
            .clipToBounds()
            .nestedScroll(state.nestedScrollConnection)
    ) {
        content()
        if (state.isRefreshing || state.progress > 0f) {
            PullToRefreshContainer(
                state = state,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * Contenedor para estados vacíos o de error dentro de [AgroPullToRefreshBox].
 * El gesto solo responde sobre algo que se pueda desplazar; sin esto una
 * pantalla vacía o con error no se podía recargar deslizando.
 */
@Composable
fun RefreshableFullScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}

// Las cargas reales arrancan en milisegundos; un segundo y medio cubre a las
// que primero leen la sesión guardada antes de llamar al servidor.
private const val START_TIMEOUT_MS = 1_500L
