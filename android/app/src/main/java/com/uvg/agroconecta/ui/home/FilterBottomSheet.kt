package com.uvg.agroconecta.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.theme.AgroSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    precioMinInicial: Int?,
    precioMaxInicial: Int?,
    marcaInicial: String,
    onAplicar: (precioMin: Int?, precioMax: Int?, marca: String) -> Unit,
    onLimpiar: () -> Unit,
    onDismiss: () -> Unit
) {
    var marca by remember { mutableStateOf(marcaInicial) }
    var precioMinStr by remember { mutableStateOf(precioMinInicial?.toString() ?: "") }
    var precioMaxStr by remember { mutableStateOf(precioMaxInicial?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AgroSpacing.lg)
                .padding(bottom = AgroSpacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Text(
                text = "Filtrar productos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            AgroTextField(
                value = marca,
                onValueChange = { marca = it },
                label = "Marca",
                placeholder = "Ej: AgroMaya",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Text(
                text = "Precio en quetzales",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                // Solo dígitos: el teclado numérico de algunos teléfonos deja
                // escribir puntos o guiones que luego no se pueden interpretar.
                AgroTextField(
                    value = precioMinStr,
                    onValueChange = { if (it.all(Char::isDigit)) precioMinStr = it },
                    label = "Desde",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )
                AgroTextField(
                    value = precioMaxStr,
                    onValueChange = { if (it.all(Char::isDigit)) precioMaxStr = it },
                    label = "Hasta",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AgroSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                AgroSecondaryButton(
                    text = "Quitar filtros",
                    onClick = {
                        marca = ""
                        precioMinStr = ""
                        precioMaxStr = ""
                        onLimpiar()
                    },
                    modifier = Modifier.weight(1f)
                )
                AgroPrimaryButton(
                    text = "Aplicar",
                    onClick = {
                        onAplicar(
                            precioMinStr.toIntOrNull(),
                            precioMaxStr.toIntOrNull(),
                            marca.trim()
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
