package com.uvg.agroconecta.ui.distributor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributorProfileScreen(
    distributorId: Int,
    onNavigateBack: () -> Unit,
    onProductoClick: (Int) -> Unit,
    viewModel: DistributorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    var mostrarTodosProductos by remember { mutableStateOf(false) }

    LaunchedEffect(distributorId) {
        val token = SessionManager.getToken(context).first()
        viewModel.loadAll(distributorId, token)
    }

    LaunchedEffect(uiState.reviewSubmitSuccess) {
        if (uiState.reviewSubmitSuccess) {
            snackbarHostState.showSnackbar("¡Reseña enviada!")
            viewModel.clearReviewMessages()
        }
    }

    LaunchedEffect(uiState.reviewSubmitError) {
        uiState.reviewSubmitError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearReviewMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.distributorName.ifBlank { "Distribuidor" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ── Header distribuidor ───────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenSurface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GreenPale),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.distributorName
                                .firstOrNull()
                                ?.uppercaseChar()
                                ?.toString() ?: "D",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimaryDark
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = uiState.distributorName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = GrayDark,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isVerified) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = GreenPrimary,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Distribuidor Verificado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    uiState.reviews.takeIf { it.isNotEmpty() }?.let {
                        val promedio = it.map { r -> r.calificacion }.average()
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "%.1f".format(promedio),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GrayDark
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "(${it.size} reseñas)",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMid
                            )
                        }
                    }
                }
            }

            // ── Productos disponibles ─────────────────────────────────────
            item {
                SectionHeader(title = "Productos disponibles")
            }

            if (uiState.productos.isEmpty()) {
                item {
                    Text(
                        text = "Este distribuidor no tiene productos publicados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayMid,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                val productosAMostrar = if (mostrarTodosProductos) {
                    uiState.productos
                } else {
                    uiState.productos.take(5)
                }

                items(productosAMostrar) { producto ->
                    ProductoDistribuidorCard(
                        producto = producto,
                        onClick = { onProductoClick(producto.id) }
                    )
                }

                if (uiState.productos.size > 5) {
                    item {
                        TextButton(
                            onClick = { mostrarTodosProductos = !mostrarTodosProductos },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = if (mostrarTodosProductos)
                                    Icons.Default.ExpandLess
                                else
                                    Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (mostrarTodosProductos)
                                    "Ver menos"
                                else
                                    "Ver más (${uiState.productos.size - 5} productos más)",
                                color = GreenPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // ── Dejar reseña ──────────────────────────────────────────────
            item {
                SectionHeader(title = "Dejar reseña")
                ReviewFormCard(
                    isSubmitting = uiState.isSubmittingReview,
                    onSubmit = { calificacion, comentario ->
                        scope.launch {
                            val token = SessionManager.getToken(context).first()
                            viewModel.submitReview(
                                distributorId,
                                calificacion,
                                comentario,
                                token
                            )
                        }
                    }
                )
            }

            // ── Reseñas existentes ────────────────────────────────────────
            item {
                SectionHeader(title = "Reseñas (${uiState.reviews.size})")
            }

            if (uiState.reviews.isEmpty()) {
                item {
                    Text(
                        text = "Aún no hay reseñas. ¡Sé el primero!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayMid,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.reviews) { review ->
                    ReviewCard(review = review)
                }
            }
        }
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = GrayDark,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ProductoDistribuidorCard(
    producto: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                producto.categoria?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMid
                    )
                }
            }
            producto.precioDesde?.let { precio ->
                Text(
                    text = "Q${"%.2f".format(precio)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }
        }
    }
}

@Composable
private fun ReviewFormCard(
    isSubmitting: Boolean,
    onSubmit: (calificacion: Int, comentario: String) -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= selectedStars) Icons.Default.Star
                        else Icons.Outlined.StarOutline,
                        contentDescription = "Estrella $i",
                        tint = if (i <= selectedStars) Color(0xFFFFC107) else GrayLight,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { selectedStars = i }
                    )
                }
                if (selectedStars > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (selectedStars) {
                            1 -> "Muy malo"
                            2 -> "Malo"
                            3 -> "Regular"
                            4 -> "Bueno"
                            5 -> "Excelente"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenPrimary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = comentario,
                onValueChange = { if (it.length <= 300) comentario = it },
                placeholder = {
                    Text(
                        "¿Cómo fue tu experiencia con este distribuidor?",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMid
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayLight,
                    cursorColor = GreenPrimary
                ),
                maxLines = 5,
                supportingText = {
                    Text(
                        "${comentario.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMid
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (selectedStars > 0) {
                        onSubmit(selectedStars, comentario.trim())
                        selectedStars = 0
                        comentario = ""
                    }
                },
                enabled = selectedStars > 0 && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Enviar reseña", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: DistributorReview) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GreenSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = review.agricultorNombre
                        ?.firstOrNull()
                        ?.uppercaseChar()
                        ?.toString() ?: "A",
                    style = MaterialTheme.typography.titleSmall,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = review.agricultorNombre ?: "Agricultor",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GrayDark
                    )
                    Text(
                        text = review.fechaResena?.take(10) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMid
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= review.calificacion) Icons.Default.Star
                            else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = if (i <= review.calificacion) Color(0xFFFFC107) else GrayLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                review.comentario?.takeIf { it.isNotBlank() }?.let { com ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = com,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayDark
                    )
                }
                review.productoNombre?.let { prod ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Producto: $prod",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMid
                    )
                }
            }
        }
    }
}