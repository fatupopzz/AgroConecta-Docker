package com.uvg.agroconecta.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.location.BusinessLocationState
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.validation.ProfileFormValidator
import java.util.Locale

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onFarmLocationClick: () -> Unit = {},
    onHelpClick: () -> Unit,
    onStatsClick: () -> Unit,
    onMyProductsClick: () -> Unit,
    tipoUsuario: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isLoggingOut by viewModel.isLoggingOut.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val businessLocationState by viewModel.businessLocationState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
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

    val currentData = (uiState as? ProfileUiState.Success)?.data
    if (showEditDialog && currentData != null) {
        EditProfileDialog(
            profileData = currentData,
            isSaving = isSaving,
            error = saveError,
            businessLocationState = businessLocationState,
            onRequestBusinessLocation = viewModel::captureBusinessLocation,
            onLocationPermissionDenied = viewModel::onBusinessLocationPermissionDenied,
            onSave = { draft ->
                viewModel.saveProfile(draft) {
                    viewModel.clearBusinessLocationState()
                    showEditDialog = false
                }
            },
            onDismiss = {
                if (!isSaving) {
                    viewModel.clearSaveError()
                    viewModel.clearBusinessLocationState()
                    showEditDialog = false
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AgroTopAppBar(title = "Mi perfil") },
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
                is ProfileUiState.Loading -> SkeletonList(
                    count = 3,
                    description = "Cargando tu perfil",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                is ProfileUiState.Error -> ErrorView(
                    message = state.message,
                    onRetry = { viewModel.loadProfile() }
                )

                is ProfileUiState.Success -> {
                    when (val data = state.data) {
                        is ProfileData.Farmer -> FarmerProfileContent(
                            data.profile,
                            onFavoritesClick = onFavoritesClick,
                            onDashboardClick = onDashboardClick,
                            onFarmLocationClick = onFarmLocationClick,
                            onHelpClick = onHelpClick,
                            onEditClick = { showEditDialog = true },
                            onLogoutClick = { showLogoutDialog = true }
                        )
                        is ProfileData.Distributor -> DistributorProfileContent(
                            data.profile,
                            onStatsClick = onStatsClick,
                            onMyProductsClick = onMyProductsClick,
                            onHelpClick = onHelpClick,
                            onEditClick = { showEditDialog = true },
                            onLogoutClick = { showLogoutDialog = true }
                        )
                    }
                }
            }

            if (isLoggingOut) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
        }
    }
}

@Composable
fun FarmerProfileContent(
    profile: FarmerProfile,
    onFavoritesClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onHelpClick: () -> Unit,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onFarmLocationClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = AgroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        ProfileHeader(
            initial = profile.nombre?.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
            title = listOfNotNull(profile.nombre, profile.apellido).joinToString(" ").ifBlank { "Agricultor" },
            subtitle = profile.email
        ) {
            if (profile.tieneMembresia == true) {
                StatusTag(text = "Miembro Premium", tone = TagTone.Warning, icon = Icons.Default.Star)
            }
        }

        ProfileShortcuts {
            FarmLocationButton(onClick = onFarmLocationClick)
            FarmerDashboardButton(onClick = onDashboardClick)
            FavoriteProductsButton(onClick = onFavoritesClick)
        }

        ProfileSection(title = "Contacto") {
            ProfileInfoRow(Icons.Default.Phone, "Teléfono", profile.telefono ?: "—")
            ProfileInfoRow(Icons.Default.Email, "Correo electrónico", profile.email ?: "—")
        }

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

        ProfileSection(title = "Tu finca") {
            ProfileInfoRow(
                Icons.Default.Agriculture,
                "Tipo de agricultor",
                when (profile.tipoAgricultor) {
                    "pequena_escala" -> "Pequeña escala"
                    "mediana_escala" -> "Mediana escala"
                    "industrial" -> "Industrial"
                    else -> "—"
                }
            )
            ProfileInfoRow(
                Icons.Default.Landscape,
                "Tamaño del terreno",
                profile.tamanoTerrenoHa?.let { "$it hectáreas" } ?: "—"
            )
            ProfileInfoRow(
                Icons.Default.Grass,
                "Cultivos principales",
                profile.cultivosPrincipales ?: "—"
            )
        }

        ProfileShortcuts {
            HelpButton(onClick = onHelpClick)
        }
        ProfileAccountActions(onEditClick = onEditClick, onLogoutClick = onLogoutClick)
    }
}

@Composable
fun DistributorProfileContent(
    profile: DistributorProfile,
    onStatsClick: () -> Unit,
    onMyProductsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = AgroSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        ProfileHeader(
            initial = profile.nombreNegocio.firstOrNull()?.uppercaseChar()?.toString() ?: "D",
            title = profile.nombreNegocio,
            subtitle = profile.email
        ) {
            if (profile.estadoVerificacion == "verificado") {
                StatusTag(text = "Distribuidor verificado", tone = TagTone.Info, icon = Icons.Default.Verified)
            }
        }

        // Mis productos es la única forma de cambiar la foto de algo ya
        // publicado; publicar solo permite elegirla para productos nuevos.
        ProfileShortcuts {
            ProfileShortcut(icon = Icons.Default.Inventory2, label = "Mis productos", onClick = onMyProductsClick)
            ProfileShortcut(icon = Icons.Default.Analytics, label = "Mis ventas", onClick = onStatsClick)
        }

        ProfileSection("Contacto") {
            ProfileInfoRow(
                Icons.Default.Person,
                "Responsable",
                listOfNotNull(profile.nombre, profile.apellido).joinToString(" ").ifBlank { "—" }
            )
            ProfileInfoRow(Icons.Default.Phone, "Teléfono", profile.telefono ?: "—")
            ProfileInfoRow(Icons.Default.Email, "Correo electrónico", profile.email ?: "—")
        }

        ProfileSection("Tu negocio") {
            ProfileInfoRow(
                Icons.Default.LocationOn,
                "Departamento",
                profile.departamento?.replaceFirstChar { it.uppercase() } ?: "—"
            )
            ProfileInfoRow(Icons.Default.Receipt, "NIT", profile.nit ?: "—")
            ProfileInfoRow(Icons.Default.Place, "Dirección", profile.direccion ?: "—")
            profile.calificacionPromedio?.let {
                ProfileInfoRow(
                    Icons.Default.Star,
                    "Calificación promedio",
                    String.format(Locale.US, "%.1f de 5", it)
                )
            }
        }

        ProfileShortcuts {
            HelpButton(onClick = onHelpClick)
        }
        ProfileAccountActions(onEditClick = onEditClick, onLogoutClick = onLogoutClick)
    }
}

@Composable
private fun ProfileHeader(
    initial: String,
    title: String,
    subtitle: String?,
    badge: @Composable () -> Unit
) {
    val onHeader = MaterialTheme.agroColors.onTopBar
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.agroColors.topBarContainer)
            .padding(vertical = AgroSpacing.lg, horizontal = AgroSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(AgroSpacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = onHeader,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = onHeader
            )
        }
        badge()
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal)
    ) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(AgroSpacing.xs))
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
            .padding(vertical = AgroSpacing.xs)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(AgroSizes.iconSmall)
            )
        }
        Spacer(Modifier.width(AgroSpacing.sm))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Grupo de accesos en una sola tarjeta, separados por líneas finas. */
@Composable
private fun ProfileShortcuts(content: @Composable ColumnScope.() -> Unit) {
    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal)
    ) {
        Column(content = content)
    }
}

/**
 * Fila de acceso a otra sección. Es un elemento de lista y no un botón con
 * borde porque varios botones apilados competían entre sí y no se sabía cuál
 * era el importante.
 */
@Composable
private fun ProfileShortcut(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClickLabel = "Abrir $label", role = Role.Button, onClick = onClick)
            .padding(horizontal = AgroSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(Modifier.width(AgroSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FavoriteProductsButton(onClick: () -> Unit) {
    ProfileShortcut(
        icon = Icons.Default.Favorite,
        label = "Mis favoritos",
        onClick = onClick,
        iconTint = MaterialTheme.colorScheme.error
    )
}

@Composable
fun FarmLocationButton(onClick: () -> Unit) {
    ProfileShortcut(icon = Icons.Default.MyLocation, label = "Ubicación de mi finca", onClick = onClick)
}

@Composable
fun FarmerDashboardButton(onClick: () -> Unit) {
    ProfileShortcut(icon = Icons.Default.Analytics, label = "Mi resumen", onClick = onClick)
}

@Composable
fun HelpButton(onClick: () -> Unit) {
    ProfileShortcut(
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        label = "Ayuda y preguntas frecuentes",
        onClick = onClick
    )
}

@Composable
private fun ProfileAccountActions(onEditClick: () -> Unit, onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
    ) {
        EditProfileButton(onClick = onEditClick)
        LogoutButton(onClick = onLogoutClick)
    }
}

@Composable
fun EditProfileButton(onClick: () -> Unit) {
    AgroSecondaryButton(
        text = "Editar mis datos",
        icon = Icons.Default.Edit,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun EditProfileDialog(
    profileData: ProfileData,
    isSaving: Boolean,
    error: String?,
    businessLocationState: BusinessLocationState = BusinessLocationState.Idle,
    onRequestBusinessLocation: () -> Unit = {},
    onLocationPermissionDenied: () -> Unit = {},
    onSave: (ProfileEditDraft) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialDraft = remember(profileData) {
        when (profileData) {
            is ProfileData.Farmer -> profileData.profile.let {
                ProfileEditDraft(
                    nombre = it.nombre.orEmpty(),
                    apellido = it.apellido.orEmpty(),
                    telefono = it.telefono.orEmpty(),
                    email = it.email.orEmpty(),
                    departamento = it.departamento.orEmpty(),
                    municipio = it.municipio.orEmpty()
                )
            }
            is ProfileData.Distributor -> profileData.profile.let {
                ProfileEditDraft(
                    nombre = it.nombre.orEmpty(),
                    apellido = it.apellido.orEmpty(),
                    telefono = it.telefono.orEmpty(),
                    email = it.email.orEmpty(),
                    departamento = it.departamento.orEmpty(),
                    nombreNegocio = it.nombreNegocio,
                    nit = it.nit.orEmpty(),
                    direccion = it.direccion.orEmpty(),
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }
        }
    }
    var draft by remember(profileData) { mutableStateOf(initialDraft) }
    val isDistributor = profileData is ProfileData.Distributor
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            onRequestBusinessLocation()
        } else {
            onLocationPermissionDenied()
        }
    }

    LaunchedEffect(businessLocationState) {
        if (businessLocationState is BusinessLocationState.Located) {
            draft = draft.copy(
                latitude = businessLocationState.coordinates.latitude,
                longitude = businessLocationState.coordinates.longitude
            )
        }
    }

    fun requestBusinessLocation() {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            onRequestBusinessLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Editar mis datos") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                AgroTextField(draft.nombre, { draft = draft.copy(nombre = it) }, label = "Nombre")
                AgroTextField(draft.apellido, { draft = draft.copy(apellido = it) }, label = "Apellido")
                AgroTextField(draft.telefono, { draft = draft.copy(telefono = it) }, label = "Teléfono")
                AgroTextField(draft.email, { draft = draft.copy(email = it) }, label = "Correo electrónico")
                AgroTextField(draft.departamento, { draft = draft.copy(departamento = it) }, label = "Departamento")
                if (isDistributor) {
                    AgroTextField(
                        draft.nombreNegocio,
                        { draft = draft.copy(nombreNegocio = it) },
                        label = "Nombre del negocio"
                    )
                    AgroTextField(draft.nit, { draft = draft.copy(nit = it) }, label = "NIT")
                    AgroTextField(draft.direccion, { draft = draft.copy(direccion = it) }, label = "Dirección")
                    AgroSecondaryButton(
                        text = if (draft.latitude != null && draft.longitude != null) {
                            "Actualizar ubicación del negocio"
                        } else {
                            "Usar mi ubicación actual"
                        },
                        loadingText = "Buscando ubicación…",
                        icon = Icons.Default.MyLocation,
                        onClick = { requestBusinessLocation() },
                        loading = businessLocationState is BusinessLocationState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    when (businessLocationState) {
                        is BusinessLocationState.Located -> Text(
                            text = "Ubicación del negocio guardada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        is BusinessLocationState.Error -> InlineError(message = businessLocationState.message)
                        else -> Unit
                    }
                } else {
                    AgroTextField(draft.municipio, { draft = draft.copy(municipio = it) }, label = "Municipio")
                }
                if (error != null) {
                    InlineError(message = error)
                }
            }
        },
        confirmButton = {
            AgroPrimaryButton(
                text = "Guardar",
                loadingText = "Guardando…",
                onClick = { onSave(draft) },
                enabled = ProfileFormValidator.isValid(
                    name = draft.nombre,
                    phone = draft.telefono
                ),
                loading = isSaving
            )
        },
        dismissButton = {
            AgroTextButton(text = "Cancelar", onClick = onDismiss, enabled = !isSaving)
        }
    )
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    AgroTextButton(
        text = "Cerrar sesión",
        icon = Icons.AutoMirrored.Filled.Logout,
        destructive = true,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    ErrorState(
        title = "No pudimos cargar tu perfil",
        message = message,
        onRetry = onRetry,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("¿Cerrar sesión?") },
        text = { Text("Para volver a entrar necesitarás tu correo y contraseña.") },
        confirmButton = {
            AgroTextButton(text = "Cerrar sesión", destructive = true, onClick = onConfirm)
        },
        dismissButton = {
            AgroTextButton(text = "Cancelar", onClick = onDismiss)
        }
    )
}
