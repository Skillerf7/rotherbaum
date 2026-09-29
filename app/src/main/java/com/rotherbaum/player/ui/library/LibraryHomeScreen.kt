package com.rotherbaum.player.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LibraryCategory(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val available: Boolean = true
)

// Reihenfolge wie bei Poweramp (Raster zeilenweise, zwei Spalten)
val libraryCategories = listOf(
    LibraryCategory("all", "Alle Titel", Icons.Filled.MusicNote, Color(0xFF6B84D6)),
    LibraryCategory("folders", "Ordner", Icons.Filled.Folder, Color(0xFF4C7BD9)),
    LibraryCategory("folderTree", "Ordnerhierarchie", Icons.Filled.FolderOpen, Color(0xFF5468DB), available = false),
    LibraryCategory("albums", "Alben", Icons.Filled.Album, Color(0xFF6A55C9)),
    LibraryCategory("artists", "Interpreten", Icons.Filled.Mic, Color(0xFF6B5C9E)),
    LibraryCategory("albumArtists", "Album-Interpreten", Icons.Filled.Mic, Color(0xFF7E5FA8)),
    LibraryCategory("genres", "Genres", Icons.Filled.LibraryMusic, Color(0xFF8A4F86), available = false),
    LibraryCategory("years", "Jahre", Icons.Filled.CalendarToday, Color(0xFF3C8A99)),
    LibraryCategory("composers", "Komponisten", Icons.Filled.Person, Color(0xFF2F8A55)),
    LibraryCategory("playlists", "Wiedergabelisten", Icons.Filled.PlaylistPlay, Color(0xFF9A5A4A)),
    LibraryCategory("streams", "Streams", Icons.Filled.Wifi, Color(0xFF6DB48A), available = false),
    LibraryCategory("queue", "Warteschlange", Icons.Filled.QueueMusic, Color(0xFF8A8A3E)),
    LibraryCategory("bookmarks", "Lesezeichen", Icons.Filled.Bookmark, Color(0xFF3F6FD0), available = false),
    LibraryCategory("mostPlayed", "Häufig gespielt", Icons.Filled.FastForward, Color(0xFF9B7BC8)),
    LibraryCategory("topRated", "Am besten bewertet", Icons.Filled.ThumbUp, Color(0xFF4F6068)),
    LibraryCategory("lowRated", "Schlecht bewertet", Icons.Filled.ThumbDown, Color(0xFF555555)),
    LibraryCategory("recentlyPlayed", "Kürzlich gespielt", Icons.Filled.History, Color(0xFF4A56B8)),
    LibraryCategory("recentlyAdded", "Kürzlich hinzugefügt", Icons.Filled.AddCircle, Color(0xFF3F6B45)),
    LibraryCategory("longTracks", "Lang", Icons.Filled.MoreHoriz, Color(0xFF5B45A8)),
    LibraryCategory("favorites", "Favoriten", Icons.Filled.Favorite, Color(0xFFB53A3A))
)

@Composable
fun LibraryHomeScreen(onCategoryClick: (LibraryCategory) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B1611), Color(0xFF050505))))
    ) {
        Text(
            "Bibliothek",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(libraryCategories) { category ->
                CategoryTile(category, onClick = { onCategoryClick(category) })
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CategoryTile(category: LibraryCategory, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = category.available) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (category.available) category.color else category.color.copy(alpha = 0.4f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(category.icon, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                category.label,
                color = if (category.available) Color.White else Color(0xFF9A9A9A),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!category.available) {
                Text("bald verfügbar", color = Color(0xFF7A7A7A), fontSize = 12.sp)
            }
        }
    }
}
