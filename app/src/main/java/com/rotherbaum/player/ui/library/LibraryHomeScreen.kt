package com.rotherbaum.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class LibraryCategory(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val available: Boolean = true
)

val libraryCategories = listOf(
    LibraryCategory("all", "Alle Titel", Icons.Filled.MusicNote, Color(0xFF5C7CFA)),
    LibraryCategory("folders", "Ordner", Icons.Filled.Folder, Color(0xFF4C6EF5)),
    LibraryCategory("albums", "Alben", Icons.Filled.Album, Color(0xFF7048E8)),
    LibraryCategory("artists", "Interpreten", Icons.Filled.Mic, Color(0xFF9C36B5)),
    LibraryCategory("albumArtists", "Album-Interpreten", Icons.Filled.Mic, Color(0xFFAE3EC9)),
    LibraryCategory("genres", "Genres", Icons.Filled.LibraryMusic, Color(0xFFC2255C), available = false),
    LibraryCategory("years", "Jahre", Icons.Filled.CalendarToday, Color(0xFF0C8599)),
    LibraryCategory("composers", "Komponisten", Icons.Filled.Person, Color(0xFF2F9E44)),
    LibraryCategory("playlists", "Wiedergabelisten", Icons.Filled.PlaylistPlay, Color(0xFFE8590C)),
    LibraryCategory("streams", "Streams", Icons.Filled.Wifi, Color(0xFF37B24D), available = false),
    LibraryCategory("queue", "Warteschlange", Icons.Filled.QueueMusic, Color(0xFF66A80F)),
    LibraryCategory("bookmarks", "Lesezeichen", Icons.Filled.Bookmark, Color(0xFF1971C2), available = false),
    LibraryCategory("mostPlayed", "Häufig gespielt", Icons.Filled.FastForward, Color(0xFFAE3EC9)),
    LibraryCategory("topRated", "Am besten bewertet", Icons.Filled.ThumbUp, Color(0xFF495057)),
    LibraryCategory("lowRated", "Schlecht bewertet", Icons.Filled.ThumbDown, Color(0xFF495057)),
    LibraryCategory("recentlyPlayed", "Kürzlich gespielt", Icons.Filled.History, Color(0xFF4263EB)),
    LibraryCategory("recentlyAdded", "Kürzlich hinzugefügt", Icons.Filled.AddCircle, Color(0xFF2B8A3E)),
    LibraryCategory("longTracks", "Lang", Icons.Filled.MoreHoriz, Color(0xFF5F3DC4)),
    LibraryCategory("favorites", "Favoriten", Icons.Filled.Favorite, Color(0xFFE03131))
)

@Composable
fun LibraryHomeScreen(onCategoryClick: (LibraryCategory) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Bibliothek",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(libraryCategories) { category ->
                CategoryTile(category, onClick = { onCategoryClick(category) })
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun CategoryTile(category: LibraryCategory, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = category.available) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (category.available) category.color else category.color.copy(alpha = 0.35f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(category.icon, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(category.label, style = MaterialTheme.typography.bodyLarge)
            if (!category.available) {
                Text(
                    "bald verfügbar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
