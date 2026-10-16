package com.uvg.agroconecta.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPasswordField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.validation.LoginFieldErrors
import com.uvg.agroconecta.validation.LoginFormValidator

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    notice: String? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // Los errores aparecen hasta que la persona intenta entrar; mostrarlos
    // mientras todavía está escribiendo se siente como un regaño.
    var fieldErrors by remember { mutableStateOf(LoginFieldErrors()) }

    val loginState by viewModel.loginState.observeAsState(AuthState.Idle)

    // Evita navegar dos veces si el estado Success se vuelve a emitir al
    // recomponer (por ejemplo, al rotar la pantalla).
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthState.Success -> {
                if (!navigated) {
                    navigated = true
                    onLoginSuccess()
                }
            }
            is AuthState.Error, is AuthState.Idle -> navigated = false
            else -> Unit
        }
    }

    val submit: () -> Unit = {
        fieldErrors = LoginFormValidator.fieldErrors(email, password)
        if (!fieldErrors.hasErrors) {
            focusManager.clearFocus()
            viewModel.login(email.trim(), password, context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrandHeader()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AgroSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandTitle(modifier = Modifier.padding(top = AgroSpacing.xxl))

            // La tarjeta se monta sobre la franja verde: el formulario queda a la
            // vista sin desplazarse y la marca no ocupa media pantalla.
            AgroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AgroSpacing.lg)
            ) {
                Column(
                    modifier = Modifier.padding(AgroSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                ) {
                    Text(
                        text = "Inicia sesión",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Entra para comprar y seguir tus pedidos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    notice?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(AgroSpacing.sm)
                            )
                        }
                    }

                    Spacer(Modifier.height(AgroSpacing.xxs))

                    AgroTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            fieldErrors = fieldErrors.copy(email = null)
                        },
                        label = "Correo electrónico",
                        placeholder = "tu@correo.com",
                        errorMessage = fieldErrors.email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    AgroPasswordField(
                        value = password,
                        onValueChange = {
                            password = it
                            fieldErrors = fieldErrors.copy(password = null)
                        },
                        label = "Contraseña",
                        errorMessage = fieldErrors.password,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )

                    (loginState as? AuthState.Error)?.let { InlineError(message = it.message) }

                    AgroPrimaryButton(
                        text = "Entrar",
                        loadingText = "Entrando…",
                        icon = Icons.AutoMirrored.Filled.Login,
                        onClick = submit,
                        loading = loginState is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AgroSpacing.xs)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AgroSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                Text(
                    text = "¿Es tu primera vez en AgroConecta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                AgroSecondaryButton(
                    text = "Crear una cuenta",
                    onClick = onNavigateToRegister,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Franja de color detrás del título; la tarjeta del formulario la monta a la mitad. */
@Composable
private fun BrandHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(
                color = MaterialTheme.agroColors.topBarContainer,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    )
}

@Composable
private fun BrandTitle(modifier: Modifier = Modifier) {
    val onBrand = MaterialTheme.agroColors.onTopBar
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = onBrand.copy(alpha = 0.15f),
            contentColor = onBrand,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(AgroSpacing.sm))
        Text(
            text = "AgroConecta",
            style = MaterialTheme.typography.headlineMedium,
            color = onBrand
        )
        Text(
            text = "Insumos agrícolas a tu alcance",
            style = MaterialTheme.typography.bodyMedium,
            color = onBrand
        )
    }
}
