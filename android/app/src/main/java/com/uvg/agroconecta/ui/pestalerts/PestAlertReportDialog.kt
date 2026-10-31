package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.validation.PestAlertReportFormValidator
import java.util.Locale

private const val MAX_DESCRIPTION_LENGTH = 500

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
    val canSubmit = PestAlertReportFormValidator.canSubmit(
        hasPestType = formState.selectedPestType != null,
        crop = formState.selectedCrop,
        hasLocation = location != null
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(text = "Reportar una plaga") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                Text(
                    text = "Tu reporte avisa a los agricultores de tu zona.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AgroDropdownField(
                    label = "Tipo de plaga",
                    options = PestType.entries,
                    selected = formState.selectedPestType,
                    onSelectedChange = onPestTypeSelected,
                    optionLabel = { it.displayName },
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag("report-pest-type")
                )

                AgroDropdownField(
                    label = "Cultivo afectado",
                    options = pestAlertCrops,
                    selected = formState.selectedCrop.ifBlank { null },
                    onSelectedChange = onCropSelected,
                    optionLabel = { it },
                    enabled = !isSubmitting,
                    modifier = Modifier.testTag("report-crop")
                )

                AgroTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChanged,
                    enabled = !isSubmitting,
                    label = "¿Qué viste? (opcional)",
                    placeholder = "Ej: hojas con manchas e insectos pequeños",
                    supportingText = "${formState.description.length}/$MAX_DESCRIPTION_LENGTH",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.testTag("report-description")
                )

                LocationSummary(
                    location = location,
                    currentLocation = currentLocation,
                    enabled = !isSubmitting,
                    onChooseLocation = onChooseLocation,
                    onUseCurrentLocation = onUseCurrentLocation
                )

                errorMessage?.let { message -> InlineError(message = message) }
            }
        },
        confirmButton = {
            AgroPrimaryButton(
                text = "Enviar reporte",
                loadingText = "Enviando…",
                onClick = onSubmit,
                enabled = canSubmit,
                loading = isSubmitting,
                modifier = Modifier.testTag("submit-pest-report")
            )
        },
        dismissButton = {
            AgroTextButton(text = "Cancelar", onClick = onDismiss, enabled = !isSubmitting)
        }
    )
}

/** Permite revisar la ubicación del reporte, elegirla en el mapa o usar el GPS. */
@Composable
private fun LocationSummary(
    location: PestAlertLocation?,
    currentLocation: PestAlertLocation?,
    enabled: Boolean,
    onChooseLocation: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    val waiting = location == null
    Surface(
        color = if (waiting) MaterialTheme.agroColors.warningContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (waiting) {
            MaterialTheme.agroColors.onWarningContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                Icon(
                    imageVector = if (waiting) Icons.Default.LocationSearching else Icons.Default.LocationOn,
                    contentDescription = null
                )
                Column {
                    Text(
                        text = "Ubicación del reporte",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = location?.let(::formatReportCoordinates)
                            ?: "Selecciona el lugar en el mapa o usa tu GPS.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                AgroTextButton(
                    text = "Elegir en mapa",
                    onClick = onChooseLocation,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("choose-report-location")
                )
                AgroTextButton(
                    text = "Usar mi GPS",
                    onClick = onUseCurrentLocation,
                    enabled = enabled && currentLocation != null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("use-current-report-location")
                )
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
