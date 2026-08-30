package com.uvg.agroconecta.ui.profile

import android.content.Context
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayBorder
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPale
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenPrimaryDark
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.OrangeLight
import com.uvg.agroconecta.ui.theme.VerifiedBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onStatsClick: () -> Unit,
    tipoUsuario: String,
    viewModel: ProfileViewModel = hiltViewModel()
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
        containerColor = GrayLight,
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedTab = BottomNavTab.PERFIL,
                tipoUsuario = tipoUsuario,
                onHomeClick = onHomeClick,
                onAgregarClick = onAgregarClick,
                onPedidosClick = onPedidosClick,
                onPerfilClick = { }
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
                        color = GreenPrimary
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
                        is ProfileData.Farmer -> FarmerProfileContent(
                            data.profile,
                            onLogoutClick = { showLogoutDialog = true }
                        )
                        is ProfileData.Distributor -> DistributorProfileContent(
                            data.profile,
                            onStatsClick = onStatsClick,
                            onLogoutClick = { showLogoutDialog = true }
                        )
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

@Composable
fun FarmerProfileContent(
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenPrimary)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(GreenPale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.nombre?.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimaryDark
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
                if (profile.tieneMembresia == true) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OrangeLight
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Miembro Premium",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangeAccent
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        ProfileSection(title = "Información de contacto") {
            ProfileInfoRow(Icons.Default.Phone, "Teléfono", profile.telefono ?: "—")
            ProfileInfoRow(Icons.Default.Email, "Correo electrónico", profile.email ?: "—")
        }

        Spacer(Modifier.height(12.dp))

        ProfileSection(title = "Ubicación") {
            ProfileInfoRow(
                Icons.Default.LocationOn,
                "Departamento",
                profile.departamento?.replaceFirstChar { it.uppercase() } ?: "—"
            )
            ProfileInfoRow(
                Icons.Default.Place,
                "Municipio",
                profile.municipio?.replaceFirstChar { it.uppercase() } ?: "—"
            )
        }

        Spacer(Modifier.height(12.dp))

        ProfileSection(title = "Datos agrícolas") {
            ProfileInfoRow(
                Icons.Default.Agriculture,
                "Tipo de agricultor",
                when (profile.tipoAgricultor) {
                    "pequena_escala" -> "Pequeña escala"
                    "mediana_escala" -> "Mediana escala"
                    "industrial"     -> "Industrial"
                    else             -> "—"
                }
            )
            ProfileInfoRow(
                Icons.Default.Landscape,
                "Tamaño del terreno",
                profile.tamanoTerrenoHa?.let { "$it ha" } ?: "—"
            )
            ProfileInfoRow(
                Icons.Default.Grass,
                "Cultivos principales",
                profile.cultivosPrincipales ?: "—"
            )
        }

        Spacer(Modifier.height(24.dp))
        LogoutButton(onClick = onLogoutClick)
    }
}

@Composable
fun DistributorProfileContent(
    profile: DistributorProfile,
    onStatsClick: () -> Unit,
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
                .background(GreenPrimary)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(GreenPale),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.nombreNegocio.firstOrNull()?.uppercaseChar()?.toString() ?: "D",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimaryDark
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    profile.nombreNegocio,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                profile.email?.let {
                    Text(it, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(8.dp))
                if (profile.estadoVerificacion == "verificado") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE3F2FD)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                null,
                                tint = VerifiedBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Distribuidor Verificado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VerifiedBlue
                            )
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
            ProfileInfoRow(
                Icons.Default.LocationOn,
                "Departamento",
                profile.departamento?.replaceFirstChar { it.uppercase() } ?: "—"
            )
            ProfileInfoRow(Icons.Default.Receipt, "NIT", profile.nit ?: "—")
            profile.calificacionPromedio?.let {
                ProfileInfoRow(Icons.Default.Star, "Calificación promedio", "%.1f / 5.0".format(it))
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onStatsClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Ver estadísticas de ventas",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }

        Spacer(Modifier.height(24.dp))
        LogoutButton(onClick = onLogoutClick)
    }
}

@Composable
fun ProfileSection(
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
                color = GrayMid,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ProfileInfoRow(
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
                .background(GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = GrayMid)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
        }
    }
    if (value != "—") HorizontalDivider(color = GrayBorder.copy(alpha = 0.5f))
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
    ) {
        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
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
            tint = GrayBorder,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(message, color = GrayMid, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = ErrorRed) },
        title = { Text("¿Cerrar sesión?") },
        text = { Text("Deberás iniciar sesión nuevamente para acceder a tu cuenta.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
            ) {
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}