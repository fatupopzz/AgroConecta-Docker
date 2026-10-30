package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.validation.PestAlertReportFormValidator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PestAlertReportDialog(
    formState: PestReportFormState,
    location: PestAlertLocation?,
    currentLocation: PestAlertLocation?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onPestTypeSelected: (PestType) -> Unit,
    onCropSelected: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onChooseLocation: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSubmit: () -> Unit
) {
    var pestMenuExpanded by remember { mutableStateOf(false) }
    var cropMenuExpanded by remember { mutableStateOf(false) }
    val canSubmit = !isSubmitting && PestAlertReportFormValidator.canSubmit(
        hasPestType = formState.selectedPestType != null,
        crop = formState.selectedCrop,
        hasLocation = location != null
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Reportar una plaga",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Comparte el reporte para alertar a agricultores cercanos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayMid
                )

                ExposedDropdownMenuBox(
                    expanded = pestMenuExpanded,
                    onExpandedChange = {
                        if (!isSubmitting) pestMenuExpanded = !pestMenuExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = formState.selectedPestType?.displayName.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSubmitting,
                        label = { Text("Tipo de plaga") },
                        placeholder = { Text("Selecciona una plaga") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = pestMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("report-pest-type")
                    )
                    ExposedDropdownMenu(
                        expanded = pestMenuExpanded,
                        onDismissRequest = { pestMenuExpanded = false }
                    ) {
                        PestType.entries.forEach { pestType ->
                            DropdownMenuItem(
                                text = { Text(pestType.displayName) },
                                onClick = {
                                    onPestTypeSelected(pestType)
                                    pestMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = cropMenuExpanded,
                    onExpandedChange = {
                        if (!isSubmitting) cropMenuExpanded = !cropMenuExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = formState.selectedCrop,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSubmitting,
                        label = { Text("Cultivo afectado") },
                        placeholder = { Text("Selecciona el cultivo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("report-crop")
                    )
                    ExposedDropdownMenu(
                        expanded = cropMenuExpanded,
                        onDismissRequest = { cropMenuExpanded = false }
                    ) {
                        pestAlertCrops.forEach { crop ->
                            DropdownMenuItem(
                                text = { Text(crop) },
                                onClick = {
                                    onCropSelected(crop)
                                    cropMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChanged,
                    enabled = !isSubmitting,
                    label = { Text("Descripción opcional") },
                    placeholder = { Text("Ejemplo: hojas con manchas y presencia de insectos") },
                    supportingText = { Text("${formState.description.length}/500") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report-description")
                )

                LocationSummary(
                    location = location,
                    currentLocation = currentLocation,
                    enabled = !isSubmitting,
                    onChooseLocation = onChooseLocation,
                    onUseCurrentLocation = onUseCurrentLocation
                )

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.testTag("submit-pest-report")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp)
                    )
                }
                Text(if (isSubmitting) "Enviando…" else "Enviar reporte")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun LocationSummary(
    location: PestAlertLocation?,
    currentLocation: PestAlertLocation?,
    enabled: Boolean,
    onChooseLocation: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    Surface(
        color = if (location == null) Color(0xFFFFF4D6) else GreenSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (location == null) Color(0xFF8A6100) else GreenPrimary
                )
                Column {
                    Text(
                        text = "Ubicación del reporte",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = location?.let(::formatReportCoordinates)
                            ?: "Selecciona el lugar en el mapa o usa tu GPS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMid
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onChooseLocation,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("choose-report-location")
                ) {
                    Text("Elegir en mapa")
                }
                TextButton(
                    onClick = onUseCurrentLocation,
                    enabled = enabled && currentLocation != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("use-current-report-location")
                ) {
                    Text("Usar mi GPS")
                }
            }
        }
    }
}

internal fun formatReportCoordinates(location: PestAlertLocation): String =
    String.format(
        Locale.US,
        "Lat. %.5f, Long. %.5f",
        location.latitude,
        location.longitude
    )
