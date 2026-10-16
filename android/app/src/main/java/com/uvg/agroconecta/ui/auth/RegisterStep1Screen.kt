package com.uvg.agroconecta.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.TipoCuenta
import com.uvg.agroconecta.ui.components.AgroPasswordField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.validation.RegisterStep1Errors
import com.uvg.agroconecta.validation.RegisterStep1FormValidator

@Composable
fun RegisterStep1Screen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNext: () -> Unit
) {
    val scrollState = rememberScrollState()
    val draft by viewModel.registerDraft.observeAsState(viewModel.registerDraft.value!!)

    var nombre by rememberSaveable { mutableStateOf(draft.nombre) }
    var apellido by rememberSaveable { mutableStateOf(draft.apellido) }
    var telefono by rememberSaveable { mutableStateOf(draft.telefono) }
    var email by rememberSaveable { mutableStateOf(draft.email) }
    var password by rememberSaveable { mutableStateOf(draft.password) }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(RegisterStep1Errors()) }

    Scaffold(
        topBar = { AgroTopAppBar(title = "Crear cuenta", onBack = onNavigateBack) }
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
            RegisterProgress(step = 1, totalSteps = 2, label = "Tus datos")

            SectionTitle("¿Cómo vas a usar AgroConecta?")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                TipoCuentaCard(
                    icon = Icons.Default.Agriculture,
                    label = "Agricultor",
                    description = "Compro insumos",
                    selected = draft.tipoCuenta == TipoCuenta.AGRICULTOR,
                    onClick = { viewModel.updateDraft { it.copy(tipoCuenta = TipoCuenta.AGRICULTOR) } },
                    modifier = Modifier.weight(1f)
                )
                TipoCuentaCard(
                    icon = Icons.Default.Storefront,
                    label = "Distribuidor",
                    description = "Vendo insumos",
                    selected = draft.tipoCuenta == TipoCuenta.DISTRIBUIDOR,
                    onClick = { viewModel.updateDraft { it.copy(tipoCuenta = TipoCuenta.DISTRIBUIDOR) } },
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle("Tus datos", modifier = Modifier.padding(top = AgroSpacing.xs))

            AgroTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    errors = errors.copy(name = null)
                },
                label = "Nombre",
                errorMessage = errors.name,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AgroTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = "Apellido (opcional)",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AgroTextField(
                value = telefono,
                onValueChange = {
                    telefono = it
                    errors = errors.copy(phone = null)
                },
                label = "Número de teléfono",
                placeholder = "5555 5555",
                supportingText = "Lo usa el distribuidor para coordinar la entrega",
                errorMessage = errors.phone,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )
            AgroTextField(
                value = email,
                onValueChange = {
                    email = it
                    errors = errors.copy(email = null)
                },
                label = "Correo electrónico",
                placeholder = "tu@correo.com",
                errorMessage = errors.email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
            AgroPasswordField(
                value = password,
                onValueChange = {
                    password = it
                    errors = errors.copy(password = null)
                },
                label = "Contraseña",
                supportingText = "Al menos 6 caracteres",
                errorMessage = errors.password,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AgroPasswordField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errors = errors.copy(passwordConfirmation = null)
                },
                label = "Repite la contraseña",
                errorMessage = errors.passwordConfirmation,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(AgroSpacing.xs))

            AgroPrimaryButton(
                text = "Siguiente",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = {
                    errors = RegisterStep1FormValidator.fieldErrors(
                        name = nombre,
                        phone = telefono,
                        email = email,
                        password = password,
                        passwordConfirmation = confirmPassword
                    )
                    if (errors.first != null) return@AgroPrimaryButton

                    viewModel.updateDraft {
                        it.copy(
                            nombre = nombre.trim(),
                            apellido = apellido.trim(),
                            telefono = telefono.trim(),
                            email = email.trim(),
                            password = password
                        )
                    }
                    onNext()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() }
    )
}

/**
 * Opción de tipo de cuenta. Se comporta como un radio (rol y estado
 * seleccionados para TalkBack) aunque se vea como tarjeta grande, que es más
 * fácil de acertar con el dedo que un círculo pequeño.
 */
@Composable
private fun TipoCuentaCard(
    icon: ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 120.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(AgroSpacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
