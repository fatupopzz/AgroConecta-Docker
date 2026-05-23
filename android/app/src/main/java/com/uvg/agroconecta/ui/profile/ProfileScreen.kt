package com.uvg.agroconecta.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// ─── Brand colors (mismos que en el resto del proyecto) ──────────────────────

private val Verde          = Color(0xFF2E7D32)
private val VerdeDark      = Color(0xFF1B5E20)
private val VerdeLight     = Color(0xFF4CAF50)
private val VerdePale      = Color(0xFFC8E6C9)
private val VerdeSurface   = Color(0xFFF1F8E9)
private val GrisFondo      = Color(0xFFF5F5F5)
private val GrisTexto      = Color(0xFF757575)
private val GrisBorde      = Color(0xFFB0BEC5)

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isLoggingOut by viewModel.isLoggingOut.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile(context)
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout(context) { onLogout() }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    Scaffold(
        containerColor = GrisFondo,
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Verde,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Verde
                    )
                }

                is ProfileUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.loadProfile(context) }
                    )
                }

                is ProfileUiState.Success -> {
                    when (val data = state.data) {
                        is ProfileData.Farmer      -> FarmerProfileContent(data.profile, onLogoutClick = { showLogoutDialog = true })
                        is ProfileData.Distributor -> DistributorProfileContent(data.profile, onLogoutClick = { showLogoutDialog = true })
                    }
                }
            }

            if (isLoggingOut) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

// ─── Profile content ──────────────────────────────────────────────────────────

@Composable
private fun FarmerProfileContent(
    profile: FarmerProfile,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header con avatar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Verde)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))

                // Avatar con inicial del nombre
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(VerdePale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.nombre?.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeDark
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = profile.nombre ?: "Agricultor",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (!profile.email.isNullOrBlank()) {
                    Text(
                        text = profile.email,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Badge membresía
                if (profile.tieneMembresia == true) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Miembro Premium",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Sección: Información de contacto ──
        ProfileSection(title = "Información de contacto") {
            ProfileInfoRow(
                icon = Icons.Default.Phone,
                label = "Teléfono",
                value = profile.telefono ?: "—"
            )
            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = "Correo electrónico",
                value = profile.email ?: "—"
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Sección: Ubicación ──
        ProfileSection(title = "Ubicación") {
            ProfileInfoRow(
                icon = Icons.Default.LocationOn,
                label = "Departamento",
                value = profile.departamento?.replaceFirstChar { it.uppercase() } ?: "—"
            )
            ProfileInfoRow(
                icon = Icons.Default.Place,
                label = "Municipio",
                value = profile.municipio?.replaceFirstChar { it.uppercase() } ?: "—"
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Sección: Datos agrícolas ──
        ProfileSection(title = "Datos agrícolas") {
            ProfileInfoRow(
                icon = Icons.Default.Agriculture,
                label = "Tipo de agricultor",
                value = when (profile.tipoAgricultor) {
                    "pequena_escala" -> "Pequeña escala"
                    "mediana_escala" -> "Mediana escala"
                    "industrial"     -> "Industrial"
                    else             -> "—"
                }
            )
            ProfileInfoRow(
                icon = Icons.Default.Landscape,
                label = "Tamaño del terreno",
                value = profile.tamanoTerrenoHa?.let { "${it} ha" } ?: "—"
            )
            ProfileInfoRow(
                icon = Icons.Default.Grass,
                label = "Cultivos principales",
                value = profile.cultivosPrincipales ?: "—"
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Botón cerrar sesión ──
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Componentes auxiliares ───────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GrisTexto,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VerdeSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Verde,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = GrisTexto)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
        }
    }
    if (value != "—") HorizontalDivider(color = GrisBorde.copy(alpha = 0.5f))
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = GrisBorde,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(message, color = GrisTexto, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Verde)
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFC62828)) },
        title = { Text("¿Cerrar sesión?") },
        text  = { Text("Deberás iniciar sesión nuevamente para acceder a tu cuenta.") },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))) {
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DistributorProfileContent(
    profile: DistributorProfile,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Verde)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(VerdePale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.nombreNegocio.firstOrNull()?.uppercaseChar()?.toString() ?: "D",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeDark
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(profile.nombreNegocio, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                profile.email?.let {
                    Text(it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(8.dp))
                if (profile.estadoVerificacion == "verificado") {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE3F2FD)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Verified, null, tint = Color(0xFF1565C0), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Distribuidor Verificado", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        ProfileSection("Información de contacto") {
            ProfileInfoRow(Icons.Default.Person, "Responsable", profile.nombre ?: "—")
            ProfileInfoRow(Icons.Default.Phone, "Teléfono", profile.telefono ?: "—")
            ProfileInfoRow(Icons.Default.Email, "Correo electrónico", profile.email ?: "—")
        }

        Spacer(Modifier.height(12.dp))

        ProfileSection("Datos del negocio") {
            ProfileInfoRow(Icons.Default.LocationOn, "Departamento", profile.departamento?.replaceFirstChar { it.uppercase() } ?: "—")
            ProfileInfoRow(Icons.Default.Receipt, "NIT", profile.nit ?: "—")
            profile.calificacionPromedio?.let {
                ProfileInfoRow(Icons.Default.Star, "Calificación promedio", "%.1f / 5.0".format(it))
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828))
        ) {
            Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
        }
    }
}