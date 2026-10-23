package com.uvg.agroconecta.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSpacing

/**
 * Barra de avance del registro. Saber cuánto falta baja la ansiedad en un
 * formulario largo, sobre todo para quien no suele llenar formularios en línea.
 */
@Composable
internal fun RegisterProgress(
    step: Int,
    totalSteps: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "Paso $step de $totalSteps: $label" }
    ) {
        Text(
            text = "Paso $step de $totalSteps · $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        LinearProgressIndicator(
            progress = { step / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AgroSpacing.xs)
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
