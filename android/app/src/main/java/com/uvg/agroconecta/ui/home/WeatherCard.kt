package com.uvg.agroconecta.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.uvg.agroconecta.data.models.WeatherDay
import com.uvg.agroconecta.data.models.WeatherReport
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.ui.theme.tabularNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val weatherLocale = Locale("es", "GT")

// Grados enteros: "22.0 °C" se lee como dato técnico y el decimal no cambia
// ninguna decisión en el campo. La lluvia sí conserva un decimal porque
// 0.5 mm y 5 mm son cosas muy distintas.
internal fun weatherDegrees(value: Double) = "${value.roundToInt()}°"
private fun weatherMillimeters(value: Double) = String.format(weatherLocale, "%.1f", value)

private fun weatherDate(value: String): String = runCatching {
    Instant.parse(value).atZone(ZoneId.of("America/Guatemala"))
        .format(DateTimeFormatter.ofPattern("dd/MM 'a las' HH:mm", weatherLocale))
}.getOrDefault("fecha no disponible")

private fun forecastDayLabel(day: WeatherDay): String = runCatching {
    LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE dd", weatherLocale))
}.getOrDefault(day.date)

// Con 50 % o más de probabilidad la decisión cambia (fumigar o no), por eso
// ese es el corte para mostrar la gota de lluvia en vez del sol.
private fun dayIcon(day: WeatherDay): ImageVector =
    if (day.rainProbability >= RAINY_DAY_PROBABILITY) Icons.Filled.WaterDrop else Icons.Filled.WbSunny

private const val RAINY_DAY_PROBABILITY = 50.0

/**
 * Clima de la finca. La temperatura y si va a llover se tienen que entender
 * de un vistazo, con ícono y número grande. El consejo para el cultivo va
 * resaltado porque es lo que cambia lo que se hace hoy. La fuente y la hora
 * del dato quedan al final, en letra pequeña, para quien quiera comprobarlas.
 */
@Composable
fun WeatherCard(state: WeatherUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    AgroCard(modifier = modifier.fillMaxWidth().testTag("weather-card")) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            WeatherHeader(state = state, onRefresh = onRefresh)

            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.report == null) {
                    Text(
                        text = "Consultando el clima…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (state.stale && state.report != null) {
                StatusTag(
                    text = "Datos desactualizados · actualiza antes de planificar labores",
                    tone = TagTone.Warning
                )
            }
            state.error?.let { InlineError(message = it) }

            val report = state.report
            if (report != null) {
                CurrentWeather(report)
                Forecast(report.forecast)
                if (report.recommendations.isNotEmpty()) {
                    Advice(
                        title = if (state.stale) "Consejos del último dato disponible" else "Consejos para tu cultivo",
                        messages = report.recommendations.map { it.message }
                    )
                }
                Text(
                    text = "Dato del ${weatherDate(report.dataTime)} · ${report.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!state.loading && state.error == null) {
                Text(
                    text = "El clima aparecerá al cargar tu ubicación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeatherHeader(state: WeatherUiState, onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Clima de tu finca",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = AgroSpacing.xs)
                .semantics { heading() }
        )
        AgroTextButton(
            text = "Actualizar",
            icon = Icons.Default.Refresh,
            onClick = onRefresh,
            enabled = !state.loading
        )
    }
    // En su propia línea: junto al botón se partía en dos renglones.
    state.location?.let {
        Text(
            text = it.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CurrentWeather(report: WeatherReport) {
    val raining = report.current.precipitationMm > 0.0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Icon(
            imageVector = if (raining) Icons.Filled.WaterDrop else Icons.Filled.WbSunny,
            contentDescription = null,
            tint = if (raining) MaterialTheme.colorScheme.tertiary else MaterialTheme.agroColors.star,
            modifier = Modifier.size(AgroSizes.iconLarge)
        )
        Column {
            Text(
                text = "${weatherDegrees(report.current.temperatureC)}C",
                style = MaterialTheme.typography.displaySmall.tabularNumbers,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (raining) {
                    "Está lloviendo: ${weatherMillimeters(report.current.precipitationMm)} mm"
                } else {
                    "Sin lluvia en este momento"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Forecast(days: List<WeatherDay>) {
    Text(
        text = "Pronóstico de 3 días",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
        days.forEach { day ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${forecastDayLabel(day)}: entre ${weatherDegrees(day.minC)} y " +
                            "${weatherDegrees(day.maxC)}, probabilidad de lluvia ${day.rainProbability.roundToInt()} por ciento"
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = AgroSpacing.sm, horizontal = AgroSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
                ) {
                    Text(
                        text = forecastDayLabel(day),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        imageVector = dayIcon(day),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${weatherDegrees(day.minC)}–${weatherDegrees(day.maxC)}",
                        style = MaterialTheme.typography.titleSmall.tabularNumbers
                    )
                    Text(
                        text = "Lluvia ${day.rainProbability.roundToInt()} %",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Advice(title: String, messages: List<String>) {
    Surface(
        color = MaterialTheme.agroColors.warningContainer,
        contentColor = MaterialTheme.agroColors.onWarningContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                messages.forEach { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
