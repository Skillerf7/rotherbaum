package com.rotherbaum.player.ui.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotherbaum.player.data.EqPresetEntity
import com.rotherbaum.player.playback.EqualizerProcessor
import com.rotherbaum.player.ui.components.EqCurve
import com.rotherbaum.player.ui.components.RbFader
import com.rotherbaum.player.ui.components.RbIconPill
import com.rotherbaum.player.ui.components.RbKnob
import com.rotherbaum.player.ui.components.RbPill
import com.rotherbaum.player.ui.components.RbTabBar
import com.rotherbaum.player.ui.components.SpectrumAnalyzer

private val Muted = Color(0xFF444444)

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
    onReloadPresets: () -> Unit,
    volume: Float,
    speed: Float,
    onVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    // Der Processor ist die "single source of truth". Dieser Zähler wird
    // beim Zeichnen gelesen, damit die Oberfläche nach jeder Änderung
    // neu gezeichnet wird.
    var refreshTick by remember { mutableStateOf(0) }
    @Suppress("UNUSED_VARIABLE")
    val tick = refreshTick

    var tab by remember { mutableStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { onReloadPresets() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF14110E), Color(0xFF050505))))
            .padding(horizontal = 12.dp)
    ) {
        RbTabBar(
            icons = listOf(Icons.Filled.Tune, Icons.Filled.Adjust, Icons.Filled.Speaker),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        )

        when (tab) {
            0 -> GraphicTab(
                equalizer = equalizer,
                audioSessionId = audioSessionId,
                presets = presets,
                onLoadPreset = onLoadPreset,
                onSaveRequest = { showSaveDialog = true },
                onChanged = { refreshTick++ }
            )
            1 -> ToneTab(
                volume = volume,
                speed = speed,
                onVolumeChange = onVolumeChange,
                onSpeedChange = onSpeedChange
            )
            else -> ReverbTab()
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

@Composable
private fun GraphicTab(
    equalizer: EqualizerProcessor,
    audioSessionId: Int,
    presets: List<EqPresetEntity>,
    onLoadPreset: (EqPresetEntity) -> Unit,
    onSaveRequest: () -> Unit,
    onChanged: () -> Unit
) {
    var presetMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // Fader: PreAmp + 10 Bänder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF101010))
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            FaderColumn(
                label = "PreAmp",
                valueText = String.format("%.1f", equalizer.preampDb),
                weight = 1.3f
            ) {
                RbFader(
                    value = equalizer.preampDb,
                    range = -24f..24f,
                    onValueChange = { equalizer.preampDb = it; onChanged() },
                    bipolarFill = true,
                    modifier = Modifier.fillMaxWidth().height(230.dp)
                )
            }
            equalizer.bands.forEachIndexed { index, band ->
                FaderColumn(
                    label = formatBandLabel(band.freqHz),
                    valueText = String.format("%.1f", band.gainDb),
                    weight = 1f
                ) {
                    RbFader(
                        value = band.gainDb,
                        range = -15f..15f,
                        onValueChange = { equalizer.setBandGain(index, it); onChanged() },
                        modifier = Modifier.fillMaxWidth().height(230.dp)
                    )
                }
            }
        }

        // Spektrum + Kurve
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(110.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1C))
        ) {
            SpectrumAnalyzer(audioSessionId = audioSessionId, modifier = Modifier.align(Alignment.BottomCenter))
            EqCurve(
                bands = equalizer.bands.map { Triple(it.freqHz, it.gainDb, it.q) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Schalter + Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RbPill(
                text = "Eq",
                onClick = { equalizer.eqEnabled = !equalizer.eqEnabled; onChanged() },
                outline = if (equalizer.eqEnabled) Color.White else Muted
            )
            RbPill(
                text = "Limit",
                onClick = { equalizer.limiterEnabled = !equalizer.limiterEnabled; onChanged() },
                outline = if (equalizer.limiterEnabled) Color(0xFFFF2A2A) else Muted
            )
            Box {
                RbPill(text = "Preset", onClick = { presetMenu = true })
                DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                    if (presets.isEmpty()) {
                        DropdownMenuItem(text = { Text("Noch keine Presets") }, onClick = { presetMenu = false })
                    }
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                onLoadPreset(preset)
                                onChanged()
                                presetMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                RbIconPill(
                    Icons.Filled.MoreVert,
                    tint = Color.White,
                    onClick = { moreMenu = true },
                    pillWidth = 52.dp
                )
                DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Preset speichern") },
                        onClick = { moreMenu = false; onSaveRequest() }
                    )
                    DropdownMenuItem(
                        text = { Text("Zurücksetzen") },
                        onClick = { equalizer.resetAll(); onChanged(); moreMenu = false }
                    )
                }
            }
        }

        // Bass & Höhen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RbKnob(
                label = "Bass",
                valueText = "${equalizer.bassPercent.toInt()}%",
                fraction = equalizer.bassPercent / 100f,
                onFractionChange = { equalizer.bassPercent = it * 100f; onChanged() },
                color = Color(0xFFFF9F0A)
            )
            RbKnob(
                label = "Höhen",
                valueText = "${equalizer.treblePercent.toInt()}%",
                fraction = equalizer.treblePercent / 100f,
                onFractionChange = { equalizer.treblePercent = it * 100f; onChanged() },
                color = Color(0xFFFF2A2A)
            )
        }
    }
}

@Composable
private fun RowScope.FaderColumn(
    label: String,
    valueText: String,
    weight: Float,
    fader: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.weight(weight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        fader()
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
        Text(valueText, color = Color(0xFFB5B5B5), fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun ToneTab(
    volume: Float,
    speed: Float,
    onVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RbKnob("Balance", "0,00", 0.5f, {}, Color.White, bipolar = true, enabled = false)
            RbKnob("Stereoerweiterung", "0%", 0f, {}, Color.White, enabled = false)
        }
        Text(
            "Balance und Stereoerweiterung folgen in einer späteren Version.",
            color = Color(0xFF7A7A7A),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RbIconPill(
                Icons.Filled.Remove,
                tint = Color.White,
                onClick = { onSpeedChange((speed - 0.05f).coerceIn(0.5f, 2f)) },
                pillWidth = 56.dp
            )
            Spacer(modifier = Modifier.weight(1f))
            RbKnob(
                label = "Tempo",
                valueText = String.format("%.2fx", speed),
                fraction = (speed - 0.5f) / 1.5f,
                onFractionChange = { onSpeedChange(0.5f + it * 1.5f) },
                color = Color(0xFF7CFF3A),
                diameter = 130.dp
            )
            Spacer(modifier = Modifier.weight(1f))
            RbIconPill(
                Icons.Filled.Add,
                tint = Color.White,
                onClick = { onSpeedChange((speed + 0.05f).coerceIn(0.5f, 2f)) },
                pillWidth = 56.dp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RbPill(text = "Mono", onClick = {}, enabled = false)
            Spacer(modifier = Modifier.weight(1f))
            RbPill(text = "Zurücksetzen", onClick = {
                onVolumeChange(1f)
                onSpeedChange(1f)
            })
        }

        Spacer(modifier = Modifier.height(12.dp))

        RbKnob(
            label = "Lautstärke",
            valueText = "${(volume * 100f).toInt()}%",
            fraction = volume,
            onFractionChange = onVolumeChange,
            color = Color(0xFF19E619),
            diameter = 190.dp
        )
    }
}

@Composable
private fun ReverbTab() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val knobs = listOf(
            "Dämpfen", "Filter", "Nachhallzeit",
            "Vor-Verzögerung", "Vor-Verzögerung Mix", "Größe"
        )
        knobs.chunked(3).forEach { rowLabels ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                rowLabels.forEach { label ->
                    RbKnob(
                        label = label,
                        valueText = "0,00",
                        fraction = 0f,
                        onFractionChange = {},
                        color = Color.White,
                        diameter = 100.dp,
                        modifier = Modifier.width(112.dp),
                        enabled = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        RbKnob("Mix", "0,00", 0f, {}, Color.White, diameter = 130.dp, enabled = false)

        Text(
            "Der Hall-Effekt folgt in einer späteren Version.",
            color = Color(0xFF7A7A7A),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
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
