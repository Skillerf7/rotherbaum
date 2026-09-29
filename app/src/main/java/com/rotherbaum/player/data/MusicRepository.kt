package com.rotherbaum.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Liest die komplette lokale Musikbibliothek über MediaStore aus -
 * kein Netzwerk, keine Cloud, alles offline auf dem Gerät.
 */
object MusicRepository {

    fun loadAllTracks(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED
        )
        // ALBUM_ARTIST/COMPOSER existieren erst ab API 30 zuverlässig
        // als Konstante - Spaltennamen notfalls direkt als String nutzen.
        val albumArtistCol = "album_artist"
        val composerCol = "composer"
        projection += albumArtistCol
        projection += composerCol

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection, projection.toTypedArray(), selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumArtistIdx = cursor.getColumnIndex(albumArtistCol)
            val composerIdx = cursor.getColumnIndex(composerCol)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: ""
                val uri = ContentUris.withAppendedId(collection, id)

                tracks += Track(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unbekannt",
                    artist = cursor.getString(artistCol) ?: "Unbekannter Interpret",
                    albumArtist = (if (albumArtistIdx >= 0) cursor.getString(albumArtistIdx) else null)
                        ?: cursor.getString(artistCol) ?: "Unbekannter Interpret",
                    album = cursor.getString(albumCol) ?: "Unbekanntes Album",
                    albumId = cursor.getLong(albumIdCol),
                    composer = (if (composerIdx >= 0) cursor.getString(composerIdx) else null)
                        ?: "Unbekannt",
                    duration = cursor.getLong(durationCol),
                    path = path,
                    folder = File(path).parent ?: "/",
                    year = cursor.getInt(yearCol).takeIf { it > 0 },
                    trackNumber = cursor.getInt(trackCol).takeIf { it > 0 },
                    dateAddedSec = cursor.getLong(dateAddedCol),
                    uri = uri
                )
            }
        }

        return tracks
    }

    fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), albumId
        )
}
