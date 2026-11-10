package app.funput.funput.keyboard.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.Executors

/**
 * The bundled sounds of the one style in use, decoded ahead of the first press.
 *
 * A [SoundPool] rather than a player per press: it keeps the sounds decoded and mixes up to
 * [MaxStreams] at once, so fast typing overlaps instead of cutting the last press short. Loading
 * reads resources and every pool call crosses into the audio service, so the pool lives on one
 * worker thread and callers only enqueue. Decoding finishes after `load` returns, and the pool
 * ignores a sample still decoding, so the press that switched styles waits for its sample.
 */
internal class KeyboardSoundPool {
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "funput-key-sounds").apply { isDaemon = true }
    }

    // Confined to the worker.
    private var pool: SoundPool? = null
    private var loadedStyle: KeyboardSoundStyle? = null
    private var soundIds: Map<KeyboardSoundKind, Int> = emptyMap()
    private val readiness = Readiness()

    fun load(context: Context, style: KeyboardSoundStyle) {
        val appContext = context.applicationContext
        worker.execute { loadOnWorker(appContext, style) }
    }

    fun play(context: Context, style: KeyboardSoundStyle, kind: KeyboardSoundKind, gain: Float) {
        val appContext = context.applicationContext
        worker.execute {
            loadOnWorker(appContext, style)
            val id = soundIds[kind] ?: return@execute
            readiness.press(id, gain)?.let { start(id, it) }
        }
    }

    private fun loadOnWorker(appContext: Context, style: KeyboardSoundStyle) {
        if (style == loadedStyle) return
        val pool = pool ?: makePool().also { pool = it }
        soundIds.values.forEach(pool::unload)
        readiness.reset()
        soundIds = KeyboardSoundKind.entries
            .mapNotNull { kind -> style.rawResource(kind)?.let { kind to pool.load(appContext, it, Priority) } }
            .toMap()
        loadedStyle = style
    }

    private fun onDecoded(id: Int, succeeded: Boolean) {
        if (!succeeded || id !in soundIds.values) return
        readiness.decoded(id)?.let { start(id, it) }
    }

    private fun start(id: Int, gain: Float) {
        pool?.play(id, gain, gain, Priority, NoLoop, NormalRate)
    }

    // The pool reports decoding on the main looper, since the worker has none.
    private fun makePool(): SoundPool = SoundPool.Builder()
        .setMaxStreams(MaxStreams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .apply {
            setOnLoadCompleteListener { _, id, status -> worker.execute { onDecoded(id, status == LoadSucceeded) } }
        }

    /** Which samples can play yet, holding back the latest press whose sample is still decoding. */
    internal class Readiness {
        private val ready = mutableSetOf<Int>()
        private var waiting: Pair<Int, Float>? = null

        /** The gain to play sample [id] at now, or null while it decodes and the press waits. */
        fun press(id: Int, gain: Float): Float? {
            if (id in ready) return gain
            waiting = id to gain
            return null
        }

        /** Marks sample [id] playable, returning the gain of a press that waited for it. */
        fun decoded(id: Int): Float? {
            ready += id
            val (waitingId, gain) = waiting ?: return null
            if (waitingId != id) return null
            waiting = null
            return gain
        }

        /** Forgets every sample, as a style switch unloads them. */
        fun reset() {
            ready.clear()
            waiting = null
        }
    }

    companion object {
        private const val MaxStreams = 4
        private const val Priority = 1
        private const val NoLoop = 0
        private const val NormalRate = 1f
        private const val LoadSucceeded = 0
    }
}
