package com.rotherbaum.player.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rotherbaum.player.ui.theme.RbBackground
import com.rotherbaum.player.ui.theme.RbSurface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.rotherbaum.player.data.PlaylistEntity
import com.rotherbaum.player.data.Track
import com.rotherbaum.player.playback.PlaybackService
import com.rotherbaum.player.playback.PlayerViewModel
import com.rotherbaum.player.ui.equalizer.EqualizerScreen
import com.rotherbaum.player.ui.equalizer.applyPreset
import com.rotherbaum.player.ui.equalizer.serializeBands
import com.rotherbaum.player.ui.library.*
import com.rotherbaum.player.ui.player.MiniPlayerBar
import com.rotherbaum.player.ui.player.PlayerScreen
import com.rotherbaum.player.ui.playlists.AddToPlaylistDialog
import com.rotherbaum.player.ui.playlists.PlaylistsScreen
import com.rotherbaum.player.ui.search.SearchScreen
import kotlinx.coroutines.launch

private sealed class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Library : BottomTab("libraryHome", "Bibliothek", Icons.Filled.GridView)
    object Equalizer : BottomTab("equalizer", "EQ", Icons.Filled.BarChart)
    object Search : BottomTab("search", "Suche", Icons.Filled.Search)
    object More : BottomTab("more", "Mehr", Icons.Filled.Menu)
}

private val bottomTabs = listOf(BottomTab.Library, BottomTab.Equalizer, BottomTab.Search, BottomTab.More)

@Composable
fun RotherbaumRoot() {
    val context = LocalContext.current
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(context))
    val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory(context))
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val allTracks by libraryViewModel.allTracks.collectAsState()
    val favoriteIds by libraryViewModel.favoriteIds.collectAsState()
    val ratings by libraryViewModel.ratings.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()

    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val shuffleEnabled by playerViewModel.shuffleEnabled.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()

    var pendingPlaylistTrack by remember { mutableStateOf<Track?>(null) }
    var presets by remember { mutableStateOf(emptyList<com.rotherbaum.player.data.EqPresetEntity>()) }

    fun playTracks(tracks: List<Track>, start: Track) {
        val index = tracks.indexOf(start).coerceAtLeast(0)
        playerViewModel.playQueue(tracks, index)
    }

    val volume by playerViewModel.volume.collectAsState()
    val speed by playerViewModel.speed.collectAsState()

    Scaffold(
        containerColor = RbBackground,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            Surface(
                color = RbSurface,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    if (currentRoute != "player") {
                        currentTrack?.let { track ->
                            MiniPlayerBar(
                                track = track,
                                isPlaying = isPlaying,
                                currentPositionMs = { playerViewModel.currentPositionMs() },
                                durationMs = { playerViewModel.durationMs() },
                                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                                onClick = { navController.navigate("player") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bottomTabs.forEach { tab ->
                            IconButton(onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(BottomTab.Library.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (currentRoute == tab.route) Color.White else Color(0xFF8A8A8A),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Library.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomTab.Library.route) {
                LibraryHomeScreen { category ->
                    navController.navigate("libraryCategory/${category.key}")
                }
            }

            composable(
                "libraryCategory/{key}",
                arguments = listOf(navArgument("key") { type = NavType.StringType })
            ) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: return@composable

                LibraryCategoryRoute(
                    key = key,
                    libraryViewModel = libraryViewModel,
                    favoriteIds = favoriteIds,
                    onTrackClick = { track, list -> playTracks(list, track) },
                    onToggleFavorite = { libraryViewModel.toggleFavorite(it.id) },
                    onAddToPlaylist = { pendingPlaylistTrack = it },
                    onOpenGroup = { groupKey ->
                        navController.navigate("libraryGroup/$key/${Uri.encode(groupKey)}")
                    }
                )
            }

            composable(
                "libraryGroup/{category}/{group}",
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType },
                    navArgument("group") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: return@composable
                val group = Uri.decode(backStackEntry.arguments?.getString("group") ?: "")

                val groupTracks = remember(category, group, allTracks) {
                    groupTracksFor(libraryViewModel, category, group)
                }

                TrackListScreen(
                    title = group,
                    tracks = groupTracks,
                    favoriteIds = favoriteIds,
                    onTrackClick = { playTracks(groupTracks, it) },
                    onToggleFavorite = { libraryViewModel.toggleFavorite(it.id) },
                    onAddToPlaylist = { pendingPlaylistTrack = it }
                )
            }

            composable(BottomTab.Equalizer.route) {
                EqualizerScreen(
                    equalizer = PlaybackService.equalizer,
                    audioSessionId = playerViewModel.audioSessionId(),
                    presets = presets,
                    onReloadPresets = {
                        coroutineScope.launch { presets = libraryViewModel.loadPresets() }
                    },
                    onSavePreset = { name ->
                        val eq = PlaybackService.equalizer
                        libraryViewModel.savePreset(
                            name = name,
                            preampDb = eq.preampDb,
                            bandsSerialized = eq.serializeBands(),
                            bassPercent = eq.bassPercent,
                            treblePercent = eq.treblePercent,
                            limiterEnabled = eq.limiterEnabled
                        )
                        coroutineScope.launch { presets = libraryViewModel.loadPresets() }
                    },
                    onLoadPreset = { preset ->
                        PlaybackService.equalizer.applyPreset(preset)
                    },
                    volume = volume,
                    speed = speed,
                    onVolumeChange = { playerViewModel.setVolume(it) },
                    onSpeedChange = { playerViewModel.setSpeed(it) }
                )
            }

            composable(BottomTab.Search.route) {
                SearchScreen(
                    search = { query, mode -> libraryViewModel.search(query, mode) },
                    favoriteIds = favoriteIds,
                    onTrackClick = { track, list -> playTracks(list, track) },
                    onToggleFavorite = { libraryViewModel.toggleFavorite(it.id) },
                    onAddToPlaylist = { pendingPlaylistTrack = it }
                )
            }

            composable(BottomTab.More.route) {
                PlaylistsScreen(
                    playlists = playlists,
                    onCreatePlaylist = { libraryViewModel.createPlaylist(it) },
                    onOpenPlaylist = { navController.navigate("playlist/${it.id}") }
                )
            }

            composable(
                "playlist/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("id") ?: return@composable
                var tracksInPlaylist by remember { mutableStateOf(emptyList<Track>()) }

                LaunchedEffect(playlistId, allTracks) {
                    tracksInPlaylist = libraryViewModel.tracksInPlaylist(playlistId)
                }

                TrackListScreen(
                    title = playlists.find { it.id == playlistId }?.name ?: "Playlist",
                    tracks = tracksInPlaylist,
                    favoriteIds = favoriteIds,
                    onTrackClick = { playTracks(tracksInPlaylist, it) },
                    onToggleFavorite = { libraryViewModel.toggleFavorite(it.id) },
                    onAddToPlaylist = { pendingPlaylistTrack = it }
                )
            }

            composable("player") {
                PlayerScreen(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    rating = currentTrack?.let { ratings[it.id] ?: 0 } ?: 0,
                    currentPositionMs = { playerViewModel.currentPositionMs() },
                    durationMs = { playerViewModel.durationMs() },
                    onTogglePlayPause = { playerViewModel.togglePlayPause() },
                    onNext = { playerViewModel.next() },
                    onPrevious = { playerViewModel.previous() },
                    onSeekTo = { playerViewModel.seekTo(it) },
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onCycleRepeat = { playerViewModel.cycleRepeatMode() },
                    onRate = { rating -> currentTrack?.let { libraryViewModel.setRating(it.id, rating) } },
                    onOpenEqualizer = {
                        navController.navigate(BottomTab.Equalizer.route) { launchSingleTop = true }
                    }
                )
            }
        }
    }

    pendingPlaylistTrack?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { pendingPlaylistTrack = null },
            onPickPlaylist = { playlist ->
                libraryViewModel.addToPlaylist(playlist.id, track.id)
                pendingPlaylistTrack = null
            },
            onCreateAndPick = { name ->
                libraryViewModel.createPlaylist(name) { newId ->
                    libraryViewModel.addToPlaylist(newId, track.id)
                }
                pendingPlaylistTrack = null
            }
        )
    }
}

@Composable
private fun LibraryCategoryRoute(
    key: String,
    libraryViewModel: LibraryViewModel,
    favoriteIds: Set<Long>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onOpenGroup: (String) -> Unit
) {
    when (key) {
        "all" -> TrackListScreen(
            "Alle Titel", libraryViewModel.allTracks.collectAsState().value, favoriteIds,
            { onTrackClick(it, libraryViewModel.allTracks.value) }, onToggleFavorite, onAddToPlaylist
        )
        "favorites" -> {
            val tracks = libraryViewModel.favoriteTracks()
            TrackListScreen("Favoriten", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "recentlyAdded" -> {
            val tracks = libraryViewModel.recentlyAdded()
            TrackListScreen("Kürzlich hinzugefügt", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "recentlyPlayed" -> {
            val tracks = libraryViewModel.recentlyPlayed()
            TrackListScreen("Kürzlich gespielt", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "mostPlayed" -> {
            val tracks = libraryViewModel.mostPlayed()
            TrackListScreen("Häufig gespielt", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "longTracks" -> {
            val tracks = libraryViewModel.longTracks()
            TrackListScreen("Lang", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "topRated" -> {
            val tracks = libraryViewModel.topRated()
            TrackListScreen("Am besten bewertet", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "lowRated" -> {
            val tracks = libraryViewModel.lowRated()
            TrackListScreen("Schlecht bewertet", tracks, favoriteIds, { onTrackClick(it, tracks) }, onToggleFavorite, onAddToPlaylist)
        }
        "folders" -> GroupListScreen("Ordner", libraryViewModel.byFolder(), { it }, onOpenGroup)
        "albums" -> GroupListScreen("Alben", libraryViewModel.byAlbum(), { it }, onOpenGroup)
        "artists" -> GroupListScreen("Interpreten", libraryViewModel.byArtist(), { it }, onOpenGroup)
        "albumArtists" -> GroupListScreen("Album-Interpreten", libraryViewModel.byAlbumArtist(), { it }, onOpenGroup)
        "composers" -> GroupListScreen("Komponisten", libraryViewModel.byComposer(), { it }, onOpenGroup)
        "years" -> GroupListScreen("Jahre", libraryViewModel.byYear(), { it.toString() }, { onOpenGroup(it.toString()) })
        "queue" -> {
            // Warteschlange zeigt schlicht die zuletzt gestartete Wiedergabeliste.
            Text("Warteschlange - aktuell über den Player-Bildschirm sichtbar.", modifier = Modifier.padding(16.dp))
        }
        "playlists" -> Text("Wiedergabelisten findest du unter \"Mehr\".", modifier = Modifier.padding(16.dp))
        else -> Text("Diese Ansicht ist noch nicht verfügbar.", modifier = Modifier.padding(16.dp))
    }
}

private fun groupTracksFor(vm: LibraryViewModel, category: String, group: String): List<Track> =
    when (category) {
        "folders" -> vm.byFolder()[group] ?: emptyList()
        "albums" -> vm.byAlbum()[group] ?: emptyList()
        "artists" -> vm.byArtist()[group] ?: emptyList()
        "albumArtists" -> vm.byAlbumArtist()[group] ?: emptyList()
        "composers" -> vm.byComposer()[group] ?: emptyList()
        "years" -> vm.byYear()[group.toIntOrNull()] ?: emptyList()
        else -> emptyList()
    }
