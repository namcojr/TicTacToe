package com.sunwings.tic_tac_toe

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Lightweight sound-effect player backed by [SoundPool].
 *
 * Respects the user's "sound_enabled" preference and loads a handful of short
 * procedurally-generated clips bundled in res/raw.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<Sound, Int> = Sound.entries.associateWith { sound ->
        soundPool.load(appContext, sound.resId, 1)
    }
    private val loaded = HashSet<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
    }

    private fun soundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)

    fun play(sound: Sound, volume: Float = 1f) {
        if (!soundEnabled()) return
        val id = ids[sound] ?: return
        if (id in loaded) {
            soundPool.play(id, volume, volume, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }

    enum class Sound(val resId: Int) {
        PLACE(R.raw.sfx_place),
        AI(R.raw.sfx_ai),
        TAP(R.raw.sfx_tap),
        WIN(R.raw.sfx_win),
        LOSE(R.raw.sfx_lose),
        DRAW(R.raw.sfx_draw)
    }
}
