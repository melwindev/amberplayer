package com.example.amberplayer // Make sure this matches!

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    // 1. Create the Player and the Session when the service starts
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()

        // This links your player to the Android OS
        mediaSession = MediaSession.Builder(this, player).build()
    }

    // 2. Android asks for this session to build the lock-screen widget
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // 3. Clean up memory when the app is completely killed
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}