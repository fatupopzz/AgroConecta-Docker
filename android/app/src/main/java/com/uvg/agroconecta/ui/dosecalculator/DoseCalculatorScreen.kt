package com.uvg.agroconecta.ui.dosecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.dosecalculator.LandUnit
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.RadioOption
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers
import com.uvg.agroconecta.validation.DoseCalculatorFormValidator
import java.text.DecimalFormat

@Composable
fun DoseCalculatorScreen(
    onHomeClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onPerfilClick: () -> Unit,
    doseCalculatorViewModel: DoseCalculatorViewModel = hiltViewModel()
) {
    val uiState by doseCalculatorViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val productSelected = DoseCalculatorFormValidator.isProductSelected(uiState.selectedProduct)

    // Es una pestaña de la barra inferior: no lleva flecha de atrás, igual que
    // Inicio o Pedidos, para que la navegación se sienta consistente.
    Scaffold(
        topBar = {
            AgroTopAppBar(
                title = "Calculadora de dosis",
                subtitle = "¿Cuánto producto necesitas?"
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedTab = BottomNavTab.AGREGAR,
                tipoUsuario = "agricultor",
                onHomeClick = onHomeClick,
                onAgregarClick = {},
                onPedidosClick = onPedidosClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            StepTitle("1. ¿Qué producto vas a usar?")
            AgroDropdownField(
                label = "Producto",
                options = uiState.products,
                selected = uiState.selectedProduct.ifBlank { null },
                onSelectedChange = doseCalculatorViewModel::onProductSelected,
                optionLabel = { it }
            )

            StepTitle("2. ¿En qué cultivo?")
            AgroDropdownField(
                label = "Cultivo",
                options = uiState.availableCrops,
                selected = uiState.selectedCrop.ifBlank { null },
                onSelectedChange = doseCalculatorViewModel::onCropSelected,
                optionLabel = { it },
                enabled = productSelected,
                supportingText = if (!productSelected) "Primero elige el producto" else null
            )

            StepTitle("3. ¿Qué tan grande es el terreno?")
            AgroTextField(
                value = uiState.landAreaInput,
                onValueChange = doseCalculatorViewModel::onLandAreaChanged,
                label = "Tamaño del terreno",
                placeholder = "Ej: 2.5",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )

            LandUnitSelector(
                selectedLandUnit = uiState.selectedLandUnit,
                onLandUnitSelected = doseCalculatorViewModel::onLandUnitSelected
            )

            uiState.errorMessage?.let { errorMessage -> InlineError(message = errorMessage) }

            AgroPrimaryButton(
                text = "Calcular dosis",
                icon = Icons.Default.Calculate,
                onClick = doseCalculatorViewModel::calculateDose,
                modifier = Modifier.fillMaxWidth()
            )

            uiState.result?.let { result ->
                DoseResultCard(
                    productName = result.productName,
                    cropName = result.cropName,
                    landAreaInHectares = result.landAreaInHectares,
                    calculatedDose = result.calculatedDose,
                    doseUnit = result.doseUnit
                )
            }

            AgroTextButton(
                text = "Empezar de nuevo",
                icon = Icons.Default.Refresh,
                onClick = doseCalculatorViewModel::resetCalculator,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Son cantidades de referencia. Lee siempre la etiqueta del fabricante antes de aplicar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = AgroSpacing.xs)
            .semantics { heading() }
    )
}

@Composable
private fun LandUnitSelector(
    selectedLandUnit: LandUnit,
    onLandUnitSelected: (LandUnit) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
    ) {
        LandUnit.entries.forEach { landUnit ->
            RadioOption(
                label = landUnit.displayName,
                selected = selectedLandUnit == landUnit,
                onClick = { onLandUnitSelected(landUnit) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DoseResultCard(
    productName: String,
    cropName: String,
    landAreaInHectares: Double,
    calculatedDose: Double,
    doseUnit: String
) {
    val numberFormat = remember { DecimalFormat("#,##0.##") }

    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = null
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Text(
                text = "Necesitas aproximadamente",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${numberFormat.format(calculatedDose)} $doseUnit",
                style = MaterialTheme.typography.displaySmall.tabularNumbers,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            ResultRow(label = "Producto", value = productName)
            ResultRow(label = "Cultivo", value = cropName)
            ResultRow(label = "Terreno", value = "${numberFormat.format(landAreaInHectares)} hectáreas")
            Spacer(modifier = Modifier.height(AgroSpacing.xxs))
            Text(
                text = "Es la cantidad total para todo el terreno.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
