package com.uvg.agroconecta.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weatherLocale = Locale("es", "GT")
private fun weatherNumber(value: Double) = String.format(weatherLocale, "%.1f", value)
private fun weatherDate(value: String): String = runCatching {
    Instant.parse(value).atZone(ZoneId.of("America/Guatemala"))
        .format(DateTimeFormatter.ofPattern("dd/MM HH:mm", weatherLocale))
}.getOrDefault("Fecha no disponible")

@Composable
fun WeatherCard(state: WeatherUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().testTag("weather-card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Clima de tu finca", modifier = Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Actualizar") }
            }
            state.location?.let { Text(it.label, style = MaterialTheme.typography.bodySmall) }
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.report == null) Text("Consultando el clima…")
            }
            if (state.stale && state.report != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                    Text("Datos desactualizados · actualiza antes de planificar labores",
                        modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val report = state.report
            if (report != null) {
                Text("${weatherNumber(report.current.temperatureC)} °C", style = MaterialTheme.typography.headlineMedium)
                Text("Lluvia actual: ${weatherNumber(report.current.precipitationMm)} mm")
                Text("Pronóstico de 3 días", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.forecast.forEach { day ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(runCatching { LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE dd", weatherLocale)) }.getOrDefault(day.date),
                                fontWeight = FontWeight.SemiBold)
                            Text("${weatherNumber(day.minC)}–${weatherNumber(day.maxC)} °C", style = MaterialTheme.typography.bodySmall)
                            Text("${weatherNumber(day.precipitationMm)} mm", style = MaterialTheme.typography.bodySmall)
                            Text("Lluvia ${day.rainProbability.toInt()} %", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (report.recommendations.isNotEmpty()) {
                    HorizontalDivider()
                    Text(if (state.stale) "Consejos del último dato disponible" else "Consejos para tu cultivo", fontWeight = FontWeight.SemiBold)
                    report.recommendations.forEach { advice -> Text(advice.message) }
                }
                Text("Dato del ${weatherDate(report.dataTime)} · ${report.source}", style = MaterialTheme.typography.bodySmall)
                Text("Consulta: ${weatherDate(report.fetchedAt)} · hora de Guatemala", style = MaterialTheme.typography.bodySmall)
            } else if (!state.loading && state.error == null) {
                Text("El clima aparecerá al cargar tu ubicación.")
            }
        }
    }
}
