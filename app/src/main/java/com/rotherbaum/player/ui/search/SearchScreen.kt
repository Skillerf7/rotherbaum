package com.rotherbaum.player.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotherbaum.player.data.Track
import com.rotherbaum.player.ui.components.RbIconPill
import com.rotherbaum.player.ui.library.TrackListScreen

private val filterLabels = listOf("Alle", "Alben", "Interpreten", "Album-Interpreten", "Ordner")

@Composable
fun SearchScreen(
    search: (String, Int) -> List<Track>,
    favoriteIds: Set<Long>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) }
    val results = remember(query, filter) { search(query, filter) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1611), Color(0xFF050505))))
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Suchen", fontSize = 22.sp, fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Leeren")
                    }
                }
            },
            shape = RoundedCornerShape(32.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2A2A2A),
                unfocusedContainerColor = Color(0xFF2A2A2A),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterLabels.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (filter == index) Color(0xFF3F3F3F) else Color(0xFF232323))
                        .clickable { filter = index }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RbIconPill(Icons.Filled.Shuffle, tint = Color.White, onClick = {
                if (results.isNotEmpty()) {
                    val mixed = results.shuffled()
                    onTrackClick(mixed.first(), mixed)
                }
            })
            RbIconPill(Icons.Filled.PlayArrow, tint = Color.White, onClick = {
                if (results.isNotEmpty()) onTrackClick(results.first(), results)
            })
        }

        TrackListScreen(
            title = if (query.isBlank()) "" else "${results.size} Treffer",
            tracks = results,
            favoriteIds = favoriteIds,
            onTrackClick = { onTrackClick(it, results) },
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onAddToPlaylist
        )
    }
}
