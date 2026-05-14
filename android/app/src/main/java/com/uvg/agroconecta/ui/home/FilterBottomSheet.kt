package com.uvg.agroconecta.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val VerdeAgroConecta = Color(0xFF2D6A1F)

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
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Filtros",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Text("Marca", fontWeight = FontWeight.Medium, fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = marca,
                onValueChange = { marca = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej: AgroMaya, InsumoGT...") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VerdeAgroConecta,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Rango de precio (Q)", fontWeight = FontWeight.Medium, fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = precioMinStr,
                    onValueChange = { if (it.all(Char::isDigit)) precioMinStr = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Mínimo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeAgroConecta,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Text("—", fontSize = 18.sp, color = Color.Gray)
                OutlinedTextField(
                    value = precioMaxStr,
                    onValueChange = { if (it.all(Char::isDigit)) precioMaxStr = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Máximo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeAgroConecta,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        marca = ""; precioMinStr = ""; precioMaxStr = ""
                        onLimpiar()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeAgroConecta)
                ) { Text("Limpiar") }

                Button(
                    onClick = {
                        onAplicar(
                            precioMinStr.toIntOrNull(),
                            precioMaxStr.toIntOrNull(),
                            marca.trim()
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeAgroConecta)
                ) { Text("Aplicar", color = Color.White) }
            }
        }
    }
}