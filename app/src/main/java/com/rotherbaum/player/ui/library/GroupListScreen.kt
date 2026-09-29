package com.rotherbaum.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Zeigt eine Liste von Gruppen (z.B. Ordnernamen oder Albumtitel),
 * jeweils mit Trackanzahl - Tap öffnet die enthaltenen Titel über
 * TrackListScreen (per Navigation).
 */
@Composable
fun <K> GroupListScreen(
    title: String,
    groups: Map<K, List<*>>,
    labelFor: (K) -> String,
    onGroupClick: (K) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        val sortedKeys = groups.keys.sortedBy { labelFor(it) }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sortedKeys) { key ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(key) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(labelFor(key), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${groups[key]?.size ?: 0} Titel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}
