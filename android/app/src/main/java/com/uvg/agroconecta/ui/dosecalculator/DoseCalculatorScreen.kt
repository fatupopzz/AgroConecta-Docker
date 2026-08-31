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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.dosecalculator.LandUnit
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import java.text.DecimalFormat

private val VerdeAgroConecta = Color(0xFF2D6A1F)
private val VerdeClaro = Color(0xFFEAF4E6)
private val GrisBorde = Color(0xFFB8B8B8)
private val RojoError = Color(0xFFB3261E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseCalculatorScreen(
    onNavigateBack: () -> Unit,
    onHomeClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onPerfilClick: () -> Unit,
    doseCalculatorViewModel: DoseCalculatorViewModel = hiltViewModel()
) {
    val uiState by doseCalculatorViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var productMenuExpanded by remember { mutableStateOf(false) }
    var cropMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calculadora de Dosis",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VerdeAgroConecta,
                    titleContentColor = Color.White
                )
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalculatorHeader()

            HorizontalDivider(color = GrisBorde)

            Text(
                text = "1. Selecciona el producto",
                style = MaterialTheme.typography.titleSmall,
                color = VerdeAgroConecta,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = productMenuExpanded,
                onExpandedChange = {
                    productMenuExpanded = !productMenuExpanded
                }
            ) {
                OutlinedTextField(
                    value = uiState.selectedProduct,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Producto") },
                    placeholder = { Text("Selecciona un producto") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = productMenuExpanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = calculatorFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = productMenuExpanded,
                    onDismissRequest = {
                        productMenuExpanded = false
                    }
                ) {
                    uiState.products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text(product) },
                            onClick = {
                                doseCalculatorViewModel.onProductSelected(product)
                                productMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "2. Selecciona el cultivo",
                style = MaterialTheme.typography.titleSmall,
                color = VerdeAgroConecta,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = cropMenuExpanded,
                onExpandedChange = {
                    if (uiState.selectedProduct.isNotBlank()) {
                        cropMenuExpanded = !cropMenuExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = uiState.selectedCrop,
                    onValueChange = {},
                    readOnly = true,
                    enabled = uiState.selectedProduct.isNotBlank(),
                    label = { Text("Cultivo") },
                    placeholder = {
                        Text(
                            if (uiState.selectedProduct.isBlank()) {
                                "Primero selecciona un producto"
                            } else {
                                "Selecciona un cultivo"
                            }
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = cropMenuExpanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = calculatorFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = cropMenuExpanded,
                    onDismissRequest = {
                        cropMenuExpanded = false
                    }
                ) {
                    uiState.availableCrops.forEach { crop ->
                        DropdownMenuItem(
                            text = { Text(crop) },
                            onClick = {
                                doseCalculatorViewModel.onCropSelected(crop)
                                cropMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = "3. Ingresa el tamaño del terreno",
                style = MaterialTheme.typography.titleSmall,
                color = VerdeAgroConecta,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = uiState.landAreaInput,
                onValueChange = doseCalculatorViewModel::onLandAreaChanged,
                label = { Text("Tamaño del terreno") },
                placeholder = { Text("Ejemplo: 2.5") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = calculatorFieldColors()
            )

            Text(
                text = "Unidad del terreno",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            LandUnitSelector(
                selectedLandUnit = uiState.selectedLandUnit,
                onLandUnitSelected = doseCalculatorViewModel::onLandUnitSelected
            )

            uiState.errorMessage?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEDEA)
                    )
                ) {
                    Text(
                        text = errorMessage,
                        color = RojoError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Button(
                onClick = doseCalculatorViewModel::calculateDose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeAgroConecta,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Calcular dosis",
                    fontWeight = FontWeight.SemiBold
                )
            }

            uiState.result?.let { result ->
                DoseResultCard(
                    productName = result.productName,
                    cropName = result.cropName,
                    landAreaInHectares = result.landAreaInHectares,
                    calculatedDose = result.calculatedDose,
                    doseUnit = result.doseUnit
                )
            }

            OutlinedButton(
                onClick = doseCalculatorViewModel::resetCalculator,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Limpiar calculadora",
                    color = VerdeAgroConecta
                )
            }

            Text(
                text = "Las cantidades mostradas son referencias generales. " +
                    "Consulta las instrucciones del fabricante antes de aplicar un producto.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CalculatorHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Calculate,
            contentDescription = null,
            tint = VerdeAgroConecta,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = "Calcula la cantidad necesaria",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Selecciona un producto, cultivo y tamaño de terreno.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun LandUnitSelector(
    selectedLandUnit: LandUnit,
    onLandUnitSelected: (LandUnit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LandUnit.entries.forEach { landUnit ->
            Card(
                onClick = {
                    onLandUnitSelected(landUnit)
                },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedLandUnit == landUnit) {
                        VerdeClaro
                    } else {
                        Color.White
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLandUnit == landUnit,
                        onClick = {
                            onLandUnitSelected(landUnit)
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = VerdeAgroConecta
                        )
                    )

                    Text(
                        text = landUnit.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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
    val numberFormat = remember {
        DecimalFormat("#,##0.##")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = VerdeClaro
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resultado recomendado",
                style = MaterialTheme.typography.titleMedium,
                color = VerdeAgroConecta,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(color = VerdeAgroConecta.copy(alpha = 0.25f))

            ResultRow(
                label = "Producto",
                value = productName
            )

            ResultRow(
                label = "Cultivo",
                value = cropName
            )

            ResultRow(
                label = "Área equivalente",
                value = "${numberFormat.format(landAreaInHectares)} ha"
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${numberFormat.format(calculatedDose)} $doseUnit",
                modifier = Modifier.fillMaxWidth(),
                color = VerdeAgroConecta,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Cantidad total estimada para el terreno ingresado.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun calculatorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VerdeAgroConecta,
    focusedLabelColor = VerdeAgroConecta,
    cursorColor = VerdeAgroConecta,
    unfocusedBorderColor = GrisBorde,
    unfocusedLabelColor = Color.Gray
)
