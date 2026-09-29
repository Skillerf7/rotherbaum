package com.rotherbaum.player.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rotherbaum.player.data.MusicRepository
import com.rotherbaum.player.data.Track
import androidx.compose.material3.Icon as M3Icon
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    track: Track?,
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    rating: Int,
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onRate: (Int) -> Unit
) {
    if (track == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kein Titel wird abgespielt.")
        }
        return
    }

    var sliderPosition by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        while (true) {
            if (!isDragging) sliderPosition = currentPositionMs().toFloat()
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        AsyncImage(
            model = MusicRepository.albumArtUri(track.albumId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(track.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${track.artist} · ${track.album}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val duration = durationMs().coerceAtLeast(1L).toFloat()
        Slider(
            value = sliderPosition.coerceIn(0f, duration),
            valueRange = 0f..duration,
            onValueChange = { isDragging = true; sliderPosition = it },
            onValueChangeFinished = { isDragging = false; onSeekTo(sliderPosition.toLong()) }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(sliderPosition.toLong()), style = MaterialTheme.typography.labelSmall)
            Text(formatMs(duration.toLong()), style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                M3Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onPrevious) {
                M3Icon(Icons.Filled.SkipPrevious, contentDescription = "Zurück", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(64.dp)) {
                M3Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onNext) {
                M3Icon(Icons.Filled.SkipNext, contentDescription = "Weiter", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onCycleRepeat) {
                M3Icon(
                    when (repeatMode) {
                        1 -> Icons.Filled.RepeatOne // Player.REPEAT_MODE_ONE
                        2 -> Icons.Filled.Repeat    // Player.REPEAT_MODE_ALL (Farbe zeigt aktiv an)
                        else -> Icons.Filled.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode != 0) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            IconButton(onClick = { onRate(if (rating == 1) 0 else 1) }) {
                M3Icon(
                    Icons.Filled.ThumbUp,
                    contentDescription = "Gut bewerten",
                    tint = if (rating == 1) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = { onRate(if (rating == -1) 0 else -1) }) {
                M3Icon(
                    Icons.Filled.ThumbDown,
                    contentDescription = "Schlecht bewerten",
                    tint = if (rating == -1) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
