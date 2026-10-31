package com.uvg.agroconecta.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.theme.AgroConectaTheme
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import kotlinx.coroutines.launch

@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onContactSupport: () -> Unit = {},
    sections: List<FaqSection> = helpFaqSections
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AgroTopAppBar(title = "Ayuda", onBack = onNavigateBack) }
    ) { innerPadding ->
        var searchQuery by rememberSaveable { mutableStateOf("") }
        val filteredSections = remember(sections, searchQuery) {
            filterFaqSections(sections, searchQuery)
        }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FaqSearchField(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    // Los resultados siempre se muestran desde el inicio de la lista
                    coroutineScope.launch { listState.scrollToItem(0) }
                },
                modifier = Modifier.padding(
                    start = AgroSpacing.screenHorizontal,
                    top = AgroSpacing.md,
                    end = AgroSpacing.screenHorizontal
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = AgroSpacing.screenHorizontal,
                    top = AgroSpacing.xs,
                    end = AgroSpacing.screenHorizontal,
                    bottom = AgroSpacing.xl
                ),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                if (searchQuery.isBlank()) {
                    item(key = "help-header") {
                        HelpHeader()
                    }
                } else if (filteredSections.isEmpty()) {
                    item(key = "faq-empty-results") {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No encontramos resultados para \"${searchQuery.trim()}\"",
                            message = "Prueba con otras palabras o escríbenos por WhatsApp."
                        )
                    }
                }

                filteredSections.forEach { section ->
                    item(key = "${section.category.name}-title") {
                        CategoryHeader(category = section.category)
                    }

                    items(
                        items = section.questions,
                        key = { faq -> "${section.category.name}:${faq.id}" }
                    ) { faq ->
                        FaqAccordionItem(faq = faq)
                    }
                }

                item(key = "whatsapp-support") {
                    WhatsAppSupportCard(onClick = onContactSupport)
                }
            }
        }
    }
}

@Composable
private fun FaqSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    AgroTextField(
        value = query,
        onValueChange = onQueryChange,
        label = "Buscar en preguntas frecuentes",
        leadingIcon = Icons.Default.Search,
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        modifier = modifier.testTag("faq-search-field")
    )
}

@Composable
private fun WhatsAppSupportCard(onClick: () -> Unit) {
    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AgroSpacing.xs),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = null
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "¿Necesitas más ayuda?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Text(
                text = "Escríbenos y el equipo de AgroConecta te ayudará.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            AgroPrimaryButton(
                text = "Escribir por WhatsApp",
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("whatsapp-support-button")
            )
        }
    }
}

@Composable
private fun HelpHeader() {
    AgroCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(AgroSizes.minTouchTarget),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
                Text(
                    text = "¿Cómo podemos ayudarte?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Toca una pregunta para ver la respuesta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: HelpCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AgroSpacing.xs, start = AgroSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
internal fun FaqAccordionItem(
    faq: FrequentlyAskedQuestion,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable(faq.id) { mutableStateOf(false) }

    AgroCard(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (isExpanded) "Respuesta abierta" else "Respuesta cerrada"
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AgroSpacing.md, vertical = AgroSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    // El estado ya lo anuncia la tarjeta; el ícono es solo visual.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = faq.answer,
                        modifier = Modifier.padding(AgroSpacing.md),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun HelpCategory.icon(): ImageVector = when (this) {
    HelpCategory.ORDERS -> Icons.Default.ShoppingBag
    HelpCategory.PAYMENTS -> Icons.Default.Payments
    HelpCategory.DELIVERIES -> Icons.Default.LocalShipping
    HelpCategory.ACCOUNT -> Icons.Default.AccountCircle
    HelpCategory.PESTS -> Icons.Default.BugReport
}

@Preview(showBackground = true)
@Composable
private fun HelpScreenPreview() {
    AgroConectaTheme(darkTheme = false) {
        HelpScreen(onNavigateBack = {})
    }
}
