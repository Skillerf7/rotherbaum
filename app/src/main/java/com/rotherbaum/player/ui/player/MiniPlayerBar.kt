package com.rotherbaum.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rotherbaum.player.data.MusicRepository
import com.rotherbaum.player.data.Track
import kotlinx.coroutines.delay

/**
 * Obere Hälfte der Dock-Karte: Cover, Titel, Interpret, Play/Pause
 * und eine Fortschrittsleiste mit Pillen-Griff.
 */
@Composable
fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    durationMs: () -> Long,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit
) {
    var fraction by remember { mutableStateOf(0f) }

    LaunchedEffect(track.id) {
        while (true) {
            val d = durationMs()
            fraction = if (d > 0L) (currentPositionMs().toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = MusicRepository.albumArtUri(track.albumId),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    track.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFB0B0B0),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF2B2B2B))
        ) {
            val thumbWidth = 42.dp
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - thumbWidth) * fraction)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFF8C8C8C))
            )
        }
    }
}
