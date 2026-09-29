package com.rotherbaum.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rotherbaum.player.MainActivity
import com.rotherbaum.player.data.RotherbaumDatabase
import com.rotherbaum.player.data.PlayHistoryEntity
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        // Eine Instanz pro Prozess, damit die EQ-Einstellwerte für
        // die UI (EqualizerScreen) direkt erreichbar sind, ohne über
        // den MediaController-Umweg zu müssen.
        val equalizer = EqualizerProcessor()

        // Audio-Session-ID des ExoPlayers (für den Spektrum-Analyzer)
        @Volatile var audioSessionId: Int = 0
    }

    override fun onCreate() {
        super.onCreate()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(equalizer)
                    )
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        audioSessionId = player.audioSessionId

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val trackId = mediaItem?.mediaId?.toLongOrNull() ?: return
                serviceScope.launch {
                    RotherbaumDatabase.get(this@PlaybackService).playHistoryDao().record(
                        PlayHistoryEntity(
                            trackId = trackId,
                            playedAtSec = System.currentTimeMillis() / 1000
                        )
                    )
                }
            }
        })

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
