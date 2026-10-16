package com.uvg.agroconecta.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.ContextCompat
import com.uvg.agroconecta.data.GuatemalaLocations
import com.uvg.agroconecta.data.location.BusinessLocationState
import com.uvg.agroconecta.data.models.TipoCuenta
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.validation.RegisterStep2Errors
import com.uvg.agroconecta.validation.RegisterStep2FormValidator

/**
 * Segundo paso del registro. Al crear la cuenta se inicia sesión de una vez
 * con los mismos datos: antes la app regresaba al login y pedía escribir otra
 * vez el correo y la contraseña que se acababan de inventar.
 *
 * [onAccountCreated] lleva a Inicio; [onAccountCreatedLoginFailed] al login
 * con un aviso, para el caso raro en que la cuenta se creó pero la sesión no.
 */
@Composable
fun RegisterStep2Screen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onAccountCreated: () -> Unit,
    onAccountCreatedLoginFailed: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val draft by viewModel.registerDraft.observeAsState(viewModel.registerDraft.value!!)
    val registerState by viewModel.registerState.observeAsState(AuthState.Idle)
    val loginState by viewModel.loginState.observeAsState(AuthState.Idle)
    val businessLocationState by viewModel.businessLocationState.observeAsState(BusinessLocationState.Idle)

    var departamento by rememberSaveable { mutableStateOf(draft.departamento ?: "") }
    var municipio by rememberSaveable { mutableStateOf(draft.municipio ?: "") }
    var nombreNegocio by rememberSaveable { mutableStateOf(draft.nombreNegocio ?: "") }
    var nit by rememberSaveable { mutableStateOf(draft.nit ?: "") }
    var errors by remember { mutableStateOf(RegisterStep2Errors()) }

    val esDistribuidor = draft.tipoCuenta == TipoCuenta.DISTRIBUIDOR
    val hasBusinessLocation = draft.latitude != null && draft.longitude != null
    val municipios = remember(departamento) { GuatemalaLocations.municipiosDe(departamento) }
    val isWorking = registerState is AuthState.Loading ||
        registerState is AuthState.Success ||
        loginState is AuthState.Loading

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.captureBusinessLocation()
        } else {
            viewModel.onBusinessLocationPermissionDenied()
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
            viewModel.captureBusinessLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(registerState) {
        if (registerState is AuthState.Success) {
            viewModel.login(draft.email, draft.password, context)
        }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthState.Success -> {
                viewModel.resetRegister()
                onAccountCreated()
            }
            is AuthState.Error -> {
                viewModel.resetRegister()
                onAccountCreatedLoginFailed()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            AgroTopAppBar(
                title = "Crear cuenta",
                onBack = onNavigateBack,
                backEnabled = !isWorking
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = AgroSpacing.lg, vertical = AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            RegisterProgress(
                step = 2,
                totalSteps = 2,
                label = if (esDistribuidor) "Tu negocio" else "Tu finca"
            )

            if (esDistribuidor) {
                SectionTitle("Datos del negocio")
                AgroTextField(
                    value = nombreNegocio,
                    onValueChange = {
                        nombreNegocio = it
                        errors = errors.copy(businessName = null)
                    },
                    label = "Nombre del negocio",
                    errorMessage = errors.businessName,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                AgroTextField(
                    value = nit,
                    onValueChange = { nit = it },
                    label = "NIT (opcional)",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                AgroSecondaryButton(
                    text = if (hasBusinessLocation) {
                        "Actualizar ubicación del negocio"
                    } else {
                        "Usar mi ubicación actual"
                    },
                    loadingText = "Buscando ubicación…",
                    onClick = {
                        errors = errors.copy(businessLocation = null)
                        requestBusinessLocation()
                    },
                    loading = businessLocationState is BusinessLocationState.Loading,
                    icon = Icons.Default.MyLocation,
                    modifier = Modifier.fillMaxWidth()
                )
                when (val locationState = businessLocationState) {
                    is BusinessLocationState.Located -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AgroSizes.iconSmall)
                        )
                        Spacer(Modifier.size(AgroSpacing.xs))
                        Text(
                            text = "Ubicación del negocio guardada",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is BusinessLocationState.Error -> InlineError(
                        message = locationState.message,
                        onRetry = { requestBusinessLocation() }
                    )
                    else -> errors.businessLocation?.let { InlineError(message = it) }
                }
            }

            SectionTitle(if (esDistribuidor) "¿Dónde está el negocio?" else "¿Dónde está tu finca?")
            Text(
                text = "Así te mostramos distribuidores y alertas de plagas cerca de ti.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AgroDropdownField(
                label = "Departamento",
                options = GuatemalaLocations.departamentos,
                selected = departamento.ifBlank { null },
                onSelectedChange = {
                    departamento = it
                    municipio = ""
                    errors = errors.copy(department = null)
                },
                optionLabel = { it },
                errorMessage = errors.department
            )
            AgroDropdownField(
                label = "Municipio",
                options = municipios,
                selected = municipio.ifBlank { null },
                onSelectedChange = {
                    municipio = it
                    errors = errors.copy(municipality = null)
                },
                optionLabel = { it },
                errorMessage = errors.municipality,
                supportingText = if (municipios.isEmpty()) "Primero elige el departamento" else null
            )

            (registerState as? AuthState.Error)?.let { InlineError(message = it.message) }

            Spacer(modifier = Modifier.height(AgroSpacing.xs))

            AgroPrimaryButton(
                text = "Crear cuenta",
                loadingText = if (loginState is AuthState.Loading) "Entrando…" else "Creando cuenta…",
                onClick = {
                    errors = RegisterStep2FormValidator.fieldErrors(
                        isDistributor = esDistribuidor,
                        businessName = nombreNegocio,
                        hasBusinessLocation = hasBusinessLocation,
                        department = departamento,
                        municipality = municipio
                    )
                    if (errors.first != null) return@AgroPrimaryButton

                    viewModel.updateDraft {
                        it.copy(
                            departamento = departamento.ifBlank { null },
                            municipio = municipio.ifBlank { null },
                            nombreNegocio = if (esDistribuidor) nombreNegocio.trim().ifBlank { null } else null,
                            nit = if (esDistribuidor) nit.trim().ifBlank { null } else null
                        )
                    }
                    viewModel.submitRegister()
                },
                loading = isWorking,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() }
    )
}
