package com.rotherbaum.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rotherbaum.player.data.MusicRepository
import com.rotherbaum.player.data.Track
import com.rotherbaum.player.ui.components.RbIconPill
import com.rotherbaum.player.ui.components.WaveSeekBar
import kotlinx.coroutines.delay

private val Inactive = Color(0xFF8A8A8A)

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
    onRate: (Int) -> Unit,
    onOpenEqualizer: () -> Unit
) {
    if (track == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kein Titel wird abgespielt.", color = Color.White)
        }
        return
    }

    var position by remember { mutableStateOf(0L) }

    LaunchedEffect(track.id) {
        while (true) {
            position = currentPositionMs()
            delay(400)
        }
    }

    val duration = durationMs().coerceAtLeast(1L)
    val fraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4A423C), Color(0xFF151515), Color(0xFF050505))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = MusicRepository.albumArtUri(track.albumId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1C1C1C))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RbIconPill(
                        Icons.Filled.ThumbUp,
                        tint = if (rating == 1) Color.White else Inactive,
                        onClick = { onRate(if (rating == 1) 0 else 1) }
                    )
                    RbIconPill(
                        Icons.Filled.ThumbDown,
                        tint = if (rating == -1) Color.White else Inactive,
                        onClick = { onRate(if (rating == -1) 0 else -1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                track.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                track.artist,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RbIconPill(Icons.Filled.GraphicEq, tint = Inactive, onClick = onOpenEqualizer)
                Spacer(modifier = Modifier.weight(1f))
                RbIconPill(
                    if (repeatMode == 1) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    tint = if (repeatMode != 0) Color.White else Inactive,
                    onClick = onCycleRepeat
                )
                Spacer(modifier = Modifier.width(8.dp))
                RbIconPill(
                    Icons.Filled.Shuffle,
                    tint = if (shuffleEnabled) Color.White else Inactive,
                    onClick = onToggleShuffle
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                WaveSeekBar(
                    seed = track.id,
                    fraction = fraction,
                    onSeek = { onSeekTo((it * duration.toFloat()).toLong()) },
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoundControl(Icons.Filled.SkipPrevious, 46.dp, onPrevious)
                    RoundControl(Icons.Filled.FastRewind, 66.dp) {
                        onSeekTo((position - 10_000L).coerceAtLeast(0L))
                    }
                    RoundControl(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        92.dp,
                        onTogglePlayPause
                    )
                    RoundControl(Icons.Filled.FastForward, 66.dp) {
                        onSeekTo((position + 10_000L).coerceAtMost(duration))
                    }
                    RoundControl(Icons.Filled.SkipNext, 46.dp, onNext)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimePill(formatMs(position))
                TimePill(formatMs(duration))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoundControl(icon: ImageVector, diameter: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(diameter * 0.45f)
        )
    }
}

@Composable
private fun TimePill(text: String) {
    Text(
        text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
