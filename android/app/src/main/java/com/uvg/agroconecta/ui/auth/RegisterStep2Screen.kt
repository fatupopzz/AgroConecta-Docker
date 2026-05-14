package com.uvg.agroconecta.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.GuatemalaLocations
import com.uvg.agroconecta.data.models.TipoCuenta
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPale
import com.uvg.agroconecta.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@Composable
fun RegisterStep2Screen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val draft by viewModel.registerDraft.observeAsState(viewModel.registerDraft.value!!)
    val registerState by viewModel.registerState.observeAsState(AuthState.Idle)

    var departamento by rememberSaveable { mutableStateOf(draft.departamento ?: "") }
    var municipio by rememberSaveable { mutableStateOf(draft.municipio ?: "") }
    var nombreNegocio by rememberSaveable { mutableStateOf(draft.nombreNegocio ?: "") }
    var nit by rememberSaveable { mutableStateOf(draft.nit ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val esDistribuidor = draft.tipoCuenta == TipoCuenta.DISTRIBUIDOR

    LaunchedEffect(registerState) {
        when (val state = registerState) {
            is AuthState.Success -> {
                snackbarHostState.showSnackbar("Cuenta creada. Inicia sesión.")
                viewModel.resetRegister()
                onRegisterSuccess()
            }
            is AuthState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(state.message) }
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(GreenPrimary)
                    .padding(start = 4.dp, top = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Text(
                        text = if (esDistribuidor) "Datos del negocio" else "Ubicación",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Paso 2 de 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenPale
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (esDistribuidor) {
                    OutlinedTextField(
                        value = nombreNegocio,
                        onValueChange = { nombreNegocio = it },
                        label = { Text("Nombre del negocio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = step2FieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nit,
                        onValueChange = { nit = it },
                        label = { Text("NIT (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = step2FieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DepartamentoDropdown(
                        selected = departamento,
                        onSelectedChange = {
                            departamento = it
                            municipio = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MunicipioDropdown(
                        departamento = departamento,
                        selected = municipio,
                        onSelectedChange = { municipio = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        error = validateStep2(esDistribuidor, nombreNegocio, departamento, municipio)
                        if (error != null) return@Button

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
                    enabled = registerState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    if (registerState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Crear cuenta", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartamentoDropdown(
    selected: String,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Departamento") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = step2FieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GuatemalaLocations.departamentos.forEach { depto ->
                DropdownMenuItem(
                    text = { Text(depto) },
                    onClick = {
                        onSelectedChange(depto)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MunicipioDropdown(
    departamento: String,
    selected: String,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val municipios = remember(departamento) { GuatemalaLocations.municipiosDe(departamento) }
    val enabled = municipios.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Municipio") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = step2FieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            municipios.forEach { mun ->
                DropdownMenuItem(
                    text = { Text(mun) },
                    onClick = {
                        onSelectedChange(mun)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun step2FieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    unfocusedBorderColor = GrayLight,
    cursorColor = GreenPrimary,
    unfocusedLabelColor = GrayMid
)

private fun validateStep2(
    esDistribuidor: Boolean,
    nombreNegocio: String,
    departamento: String,
    municipio: String
): String? {
    if (esDistribuidor && nombreNegocio.isBlank()) return "Ingresa el nombre del negocio"
    if (departamento.isBlank()) return "Selecciona un departamento"
    if (municipio.isBlank()) return "Selecciona un municipio"
    return null
}