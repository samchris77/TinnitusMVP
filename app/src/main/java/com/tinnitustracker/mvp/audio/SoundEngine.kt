package com.tinnitustracker.mvp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import com.tinnitustracker.mvp.data.Sound
import java.util.concurrent.Executors

/**
 * Feeds the [Mixer] into one AudioTrack on a dedicated thread. The thread starts on demand
 * ([wake]) and parks itself after a second of silence, so nothing runs while nothing plays.
 */
class SoundEngine(context: Context) {
    private val app = context.applicationContext
    val mixer = Mixer(SAMPLE_RATE)

    private val lock = Any()
    private var running = false

    private val decoder = Executors.newSingleThreadExecutor { Thread(it, "nature-decoder") }
    private val cache = object : LinkedHashMap<Sound, ShortArray>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Sound, ShortArray>?) = size > CACHED_LOOPS
    }

    @Volatile private var selected: Sound? = null

    /** Makes [sound] the playing source. Nature loops are decoded once, off the main thread. */
    fun select(sound: Sound) {
        if (sound == selected) return
        selected = sound
        val res = sound.rawRes
        if (res == null) {
            mixer.setSource(StereoNoise(noiseKind(sound), System.nanoTime()))
            return
        }
        val cached = synchronized(cache) { cache[sound] }
        if (cached != null) {
            mixer.setSource(LoopSource(cached))
            return
        }
        decoder.execute {
            val loop = load(sound)
            if (selected != sound) return@execute
            if (loop != null) mixer.setSource(LoopSource(loop)) else selected = null   // let a later select retry
        }
    }

    /** Decodes a nature sound ahead of time so pressing play starts it at once. */
    fun prefetch(sound: Sound) {
        if (sound.rawRes != null) decoder.execute { load(sound) }
    }

    /** Starts the audio thread if it is parked. Call after changing the mixer. */
    fun wake() {
        synchronized(lock) {
            if (running) return
            running = true
        }
        Thread(::run, "sound-engine").start()
    }

    private fun load(sound: Sound): ShortArray? {
        synchronized(cache) { cache[sound] }?.let { return it }
        return try {
            val pcm = NatureDecoder.decodeStereo(app, sound.rawRes!!, SAMPLE_RATE)
            Loops.seamless(pcm, LOOP_FADE_FRAMES, channels = 2).also { synchronized(cache) { cache[sound] = it } }
        } catch (e: Exception) {
            Log.e(TAG, "Could not decode ${sound.id}", e)
            null
        }
    }

    private fun run() {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
        val block = FloatArray(BLOCK * 2)   // interleaved stereo
        var quietBlocks = 0
        var blocks = 0
        var track: AudioTrack? = null
        try {
            track = buildTrack()
            var queued = -1
            var underruns = 0
            track.play()
            while (true) {
                // The finder's tone must follow the slider closely; a therapy sound only has to
                // keep going, so it gets a deep buffer that rides out stalls without a gap.
                val want = if (mixer.probe == Mixer.Probe.OFF) THERAPY_BUFFER_FRAMES else FINDER_BUFFER_FRAMES
                if (want != queued) {
                    track.setBufferSizeInFrames(want)
                    queued = want
                }
                mixer.render(block)
                track.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING)
                if (++blocks % PARK_AFTER_BLOCKS == 0) {
                    val count = track.underrunCount
                    if (count > underruns) Log.w(TAG, "Audio underrun: ${count - underruns} more ($count in all)")
                    underruns = count
                }
                quietBlocks = if (mixer.isIdle) quietBlocks + 1 else 0
                if (quietBlocks >= PARK_AFTER_BLOCKS) {
                    val park = synchronized(lock) {
                        if (mixer.isIdle) running = false
                        !running
                    }
                    if (park) break
                    quietBlocks = 0
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio thread stopped", e)
            synchronized(lock) { running = false }
        } finally {
            track?.let {
                runCatching { it.stop() }
                it.release()
            }
        }
    }

    private fun buildTrack(): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBytes, THERAPY_BUFFER_FRAMES * 2 * Float.SIZE_BYTES))
            .build()
    }

    private fun noiseKind(sound: Sound) = when (sound) {
        Sound.WHITE -> NoiseSource.Kind.WHITE
        Sound.BROWN -> NoiseSource.Kind.BROWN
        else -> NoiseSource.Kind.PINK
    }

    companion object {
        const val SAMPLE_RATE = 48_000
        private const val BLOCK = 1024
        private const val PARK_AFTER_BLOCKS = SAMPLE_RATE / BLOCK      // about one second
        private const val THERAPY_BUFFER_FRAMES = SAMPLE_RATE / 4      // 250 ms queued while a sound plays
        private const val FINDER_BUFFER_FRAMES = BLOCK * 4             // about 85 ms, so the tone follows the slider
        private const val LOOP_FADE_FRAMES = SAMPLE_RATE * 2           // 2 s crossfade at the loop point
        private const val CACHED_LOOPS = 2
        private const val TAG = "SoundEngine"
    }
}
