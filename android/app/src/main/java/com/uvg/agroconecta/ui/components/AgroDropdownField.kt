package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics

/**
 * Lista desplegable con el mismo aspecto que [AgroTextField]. Se prefiere a
 * escribir a mano cuando las opciones son conocidas (departamentos,
 * categorías): evita errores de ortografía y es más rápido con el pulgar.
 *
 * Cuando [options] está vacía el campo se desactiva y [placeholder] explica
 * qué falta elegir antes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AgroDropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelectedChange: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val canOpen = enabled && options.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && canOpen,
        onExpandedChange = { if (canOpen) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = canOpen,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            trailingIcon = {
                if (errorMessage != null) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && canOpen)
                }
            },
            supportingText = when {
                errorMessage != null -> {
                    { Text(errorMessage) }
                }
                supportingText != null -> {
                    { Text(supportingText) }
                }
                else -> null
            },
            isError = errorMessage != null,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
            colors = agroTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { if (errorMessage != null) error(errorMessage) }
        )
        ExposedDropdownMenu(
            expanded = expanded && canOpen,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
