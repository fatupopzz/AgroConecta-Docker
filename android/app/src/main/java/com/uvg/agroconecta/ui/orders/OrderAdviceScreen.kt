package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.AdviceMessage
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.formatFriendlyDateTime
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing

private const val MAX_ADVICE_LENGTH = 1000

@Composable
fun OrderAdviceScreen(
    orderId: Int,
    currentUserId: Int,
    messages: List<AdviceMessage>,
    isLoading: Boolean,
    isSending: Boolean,
    errorMessage: String?,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onBack: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            AgroTopAppBar(
                title = "Asesoría técnica",
                subtitle = "Pedido #$orderId",
                onBack = onBack
            )
        },
        bottomBar = {
            AdviceComposer(
                value = draft,
                isSending = isSending,
                onValueChange = { if (it.length <= MAX_ADVICE_LENGTH) draft = it },
                onSend = {
                    val message = draft.trim()
                    if (message.isNotEmpty()) {
                        onSendMessage(message)
                        draft = ""
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> SkeletonList(
                    count = 3,
                    description = "Cargando mensajes",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                messages.isEmpty() && errorMessage != null -> ErrorState(
                    title = "No pudimos cargar la conversación",
                    message = errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )

                messages.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Empieza la conversación",
                    message = "Pregunta lo que necesites sobre este pedido: cómo aplicar el producto, dosis o cuidados.",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                ) {
                    errorMessage?.let { message ->
                        item(key = "error") {
                            InlineError(message = message, onRetry = onDismissError, retryLabel = "Cerrar")
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        AdviceBubble(message, message.senderUserId == currentUserId)
                    }
                }
            }
        }
    }
}

/**
 * Burbuja de mensaje. Las propias van a la derecha en verde claro y las del
 * otro lado a la izquierda en gris, igual que en WhatsApp, que es el chat que
 * conoce la mayoría.
 */
@Composable
private fun AdviceBubble(message: AdviceMessage, isMine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (isMine) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    MaterialTheme.shapes.large
                )
                .padding(AgroSpacing.sm)
                .semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = if (isMine) "Tú" else "${message.senderName} · ${message.senderType}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isMine) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(Modifier.height(AgroSpacing.xxs))
            Text(
                text = message.mensaje,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(AgroSpacing.xxs))
            Text(
                text = formatFriendlyDateTime(message.sentAt).orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdviceComposer(
    value: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = AgroSpacing.sm, vertical = AgroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgroTextField(
                value = value,
                onValueChange = onValueChange,
                label = "Tu mensaje",
                supportingText = "${value.length}/$MAX_ADVICE_LENGTH",
                singleLine = false,
                maxLines = 4,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(AgroSpacing.xs))
            if (isSending) {
                Box(Modifier.size(AgroSizes.minTouchTarget), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(AgroSizes.iconMedium), strokeWidth = 2.dp)
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(AgroSizes.minTouchTarget)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje")
                }
            }
        }
    }
}
