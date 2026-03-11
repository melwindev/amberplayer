package com.example.amberplayer

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?
)