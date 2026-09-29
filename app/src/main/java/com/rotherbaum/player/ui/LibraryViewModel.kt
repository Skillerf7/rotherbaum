package com.rotherbaum.player.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rotherbaum.player.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ab dieser Dauer (Sekunden) gilt ein Titel als "lang" (Hörbuch/Podcast-Filter). */
private const val LONG_TRACK_SECONDS = 20 * 60

class LibraryViewModel(private val appContext: Context) : ViewModel() {

    private val db = RotherbaumDatabase.get(appContext)

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds

    private val _ratings = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val ratings: StateFlow<Map<Long, Int>> = _ratings

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists

    private val _recentlyPlayedIds = MutableStateFlow<List<Long>>(emptyList())
    private val _mostPlayedIds = MutableStateFlow<List<Long>>(emptyList())

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { MusicRepository.loadAllTracks(appContext) }
            _allTracks.value = tracks
            refreshDbState()
        }
    }

    private fun refreshDbState() {
        viewModelScope.launch {
            _favoriteIds.value = withContext(Dispatchers.IO) { db.favoriteDao().getAll().toSet() }
            _ratings.value = withContext(Dispatchers.IO) {
                db.ratingDao().getAll().associate { it.trackId to it.rating }
            }
            _playlists.value = withContext(Dispatchers.IO) { db.playlistDao().getPlaylists() }
            _recentlyPlayedIds.value = withContext(Dispatchers.IO) { db.playHistoryDao().recentlyPlayedTrackIds() }
            _mostPlayedIds.value = withContext(Dispatchers.IO) { db.playHistoryDao().mostPlayedTrackIds() }
        }
    }

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_favoriteIds.value.contains(trackId)) {
                db.favoriteDao().remove(trackId)
            } else {
                db.favoriteDao().add(FavoriteEntity(trackId))
            }
            withContext(Dispatchers.Main) { refreshDbState() }
        }
    }

    fun setRating(trackId: Long, rating: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.ratingDao().setRating(RatingEntity(trackId, rating))
            withContext(Dispatchers.Main) { refreshDbState() }
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = db.playlistDao().createPlaylist(PlaylistEntity(name = name))
            withContext(Dispatchers.Main) {
                refreshDbState()
                onCreated(id)
            }
        }
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = db.playlistDao().getTrackIds(playlistId)
            db.playlistDao().addTrack(
                PlaylistTrackEntity(playlistId, trackId, existing.size)
            )
        }
    }

    suspend fun tracksInPlaylist(playlistId: Long): List<Track> = withContext(Dispatchers.IO) {
        val ids = db.playlistDao().getTrackIds(playlistId).toSet()
        _allTracks.value.filter { it.id in ids }
    }

    fun savePreset(
        name: String,
        preampDb: Float,
        bandsSerialized: String,
        bassPercent: Float,
        treblePercent: Float,
        limiterEnabled: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            db.eqPresetDao().save(
                EqPresetEntity(
                    name = name,
                    preampDb = preampDb,
                    bandsSerialized = bandsSerialized,
                    bassPercent = bassPercent,
                    treblePercent = treblePercent,
                    limiterEnabled = limiterEnabled
                )
            )
        }
    }

    suspend fun loadPresets(): List<EqPresetEntity> = withContext(Dispatchers.IO) {
        db.eqPresetDao().getAll()
    }

    // ---- Abgeleitete Bibliotheksansichten ----

    fun favoriteTracks(): List<Track> =
        _allTracks.value.filter { it.id in _favoriteIds.value }

    fun byFolder(): Map<String, List<Track>> =
        _allTracks.value.groupBy { it.folder }

    fun byAlbum(): Map<String, List<Track>> =
        _allTracks.value.groupBy { it.album }

    fun byArtist(): Map<String, List<Track>> =
        _allTracks.value.groupBy { it.artist }

    fun byAlbumArtist(): Map<String, List<Track>> =
        _allTracks.value.groupBy { it.albumArtist }

    fun byComposer(): Map<String, List<Track>> =
        _allTracks.value.filter { it.composer != "Unbekannt" }.groupBy { it.composer }

    fun byYear(): Map<Int, List<Track>> =
        _allTracks.value.filter { it.year != null }.groupBy { it.year!! }

    fun recentlyAdded(): List<Track> =
        _allTracks.value.sortedByDescending { it.dateAddedSec }.take(200)

    fun recentlyPlayed(): List<Track> {
        val order = _recentlyPlayedIds.value
        val byId = _allTracks.value.associateBy { it.id }
        return order.mapNotNull { byId[it] }
    }

    fun mostPlayed(): List<Track> {
        val order = _mostPlayedIds.value
        val byId = _allTracks.value.associateBy { it.id }
        return order.mapNotNull { byId[it] }
    }

    fun longTracks(): List<Track> =
        _allTracks.value.filter { it.duration / 1000 >= LONG_TRACK_SECONDS }

    fun topRated(): List<Track> =
        _allTracks.value.filter { (_ratings.value[it.id] ?: 0) > 0 }

    fun lowRated(): List<Track> =
        _allTracks.value.filter { (_ratings.value[it.id] ?: 0) < 0 }

    fun search(query: String, mode: Int = 0): List<Track> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return _allTracks.value.filter {
            when (mode) {
                1 -> it.album.lowercase().contains(q)
                2 -> it.artist.lowercase().contains(q)
                3 -> it.albumArtist.lowercase().contains(q)
                4 -> it.folder.lowercase().contains(q)
                else -> it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(context.applicationContext) as T
    }
}
