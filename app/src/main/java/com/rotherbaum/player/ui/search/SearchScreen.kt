package com.rotherbaum.player.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rotherbaum.player.data.Track
import com.rotherbaum.player.ui.library.TrackListScreen

@Composable
fun SearchScreen(
    search: (String) -> List<Track>,
    favoriteIds: Set<Long>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { search(query) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Suchen") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )

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
