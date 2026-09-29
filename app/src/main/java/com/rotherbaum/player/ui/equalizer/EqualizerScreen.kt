package com.rotherbaum.player.ui.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotherbaum.player.data.EqPresetEntity
import com.rotherbaum.player.playback.EqualizerProcessor
import com.rotherbaum.player.ui.components.RotaryKnob
import com.rotherbaum.player.ui.components.SpectrumAnalyzer
import com.rotherbaum.player.ui.components.VerticalBandFader
import kotlinx.coroutines.launch

private fun formatBandLabel(freqHz: Float): String =
    if (freqHz >= 1000f) "${(freqHz / 1000f).let { if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString() }}K"
    else freqHz.toInt().toString()

@Composable
fun EqualizerScreen(
    equalizer: EqualizerProcessor,
    audioSessionId: Int,
    presets: List<EqPresetEntity>,
    onSavePreset: (name: String) -> Unit,
    onLoadPreset: (EqPresetEntity) -> Unit,
    onReloadPresets: () -> Unit
) {
    // Nur zum Neuzeichnen der UI bei Änderungen am Processor-Objekt -
    // der Processor selbst ist die "single source of truth" für den
    // laufenden Audio-Pfad.
    var refreshTick by remember { mutableStateOf(0) }
    fun bump() { refreshTick++ }

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { onReloadPresets() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Equalizer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))
        SpectrumAnalyzer(audioSessionId = audioSessionId)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RotaryKnob(
                label = "Preamp",
                value = equalizer.preampDb,
                valueRange = -24f..24f,
                valueText = String.format("%.1f dB", equalizer.preampDb),
                onValueChange = { equalizer.preampDb = it; bump() }
            )
            RotaryKnob(
                label = "Bass",
                value = equalizer.bassPercent,
                valueRange = 0f..100f,
                valueText = "${equalizer.bassPercent.toInt()}%",
                onValueChange = { equalizer.bassPercent = it; bump() }
            )
            RotaryKnob(
                label = "Höhen",
                value = equalizer.treblePercent,
                valueRange = 0f..100f,
                valueText = "${equalizer.treblePercent.toInt()}%",
                onValueChange = { equalizer.treblePercent = it; bump() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            equalizer.bands.forEachIndexed { index, band ->
                VerticalBandFader(
                    label = formatBandLabel(band.freqHz),
                    valueDb = band.gainDb,
                    onValueChange = { equalizer.setBandGain(index, it); bump() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Limiter", modifier = Modifier.weight(1f))
            Switch(
                checked = equalizer.limiterEnabled,
                onCheckedChange = { equalizer.limiterEnabled = it; bump() }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("EQ aktiv", modifier = Modifier.weight(1f))
            Switch(
                checked = equalizer.eqEnabled,
                onCheckedChange = { equalizer.eqEnabled = it; bump() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showSaveDialog = true }) { Text("Preset speichern") }
            OutlinedButton(onClick = {
                equalizer.resetAll()
                bump()
            }) { Text("Zurücksetzen") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Presets", style = MaterialTheme.typography.titleSmall)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { preset ->
                AssistChip(onClick = { onLoadPreset(preset) }, label = { Text(preset.name) })
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Preset speichern") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    singleLine = true,
                    placeholder = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (presetName.isNotBlank()) {
                        onSavePreset(presetName)
                        presetName = ""
                        showSaveDialog = false
                    }
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

/** Serialisiert die Bänder für die Presets-Tabelle: "freq:gain:q,freq:gain:q,...". */
fun EqualizerProcessor.serializeBands(): String =
    bands.joinToString(",") { "${it.freqHz}:${it.gainDb}:${it.q}" }

fun EqualizerProcessor.applyPreset(preset: EqPresetEntity) {
    preampDb = preset.preampDb
    bassPercent = preset.bassPercent
    treblePercent = preset.treblePercent
    limiterEnabled = preset.limiterEnabled
    preset.bandsSerialized.split(",").forEachIndexed { index, entry ->
        val parts = entry.split(":")
        if (parts.size == 3 && index < bands.size) {
            setBandGain(index, parts[1].toFloatOrNull() ?: 0f)
            setBandQ(index, parts[2].toFloatOrNull() ?: 1f)
        }
    }
}
