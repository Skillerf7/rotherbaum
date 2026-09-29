package com.rotherbaum.player.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val albumId: Long,
    val composer: String,
    val duration: Long,
    val path: String,
    val folder: String,
    val year: Int?,
    val trackNumber: Int?,
    val dateAddedSec: Long,
    val uri: Uri
)
