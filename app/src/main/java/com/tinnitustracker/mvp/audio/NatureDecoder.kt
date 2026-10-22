package com.tinnitustracker.mvp.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.annotation.RawRes
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

/** Decodes a compressed audio resource to interleaved stereo 16-bit PCM in memory. */
object NatureDecoder {
    private const val TIMEOUT_US = 10_000L
    private const val MAX_IDLE_POLLS = 500

    /**
     * The first two channels of [res] as interleaved stereo, resampled to [sampleRate] if the file
     * uses another rate. A mono file is copied to both channels.
     */
    fun decodeStereo(context: Context, @RawRes res: Int, sampleRate: Int): ShortArray {
        val extractor = MediaExtractor()
        try {
            context.resources.openRawResourceFd(res).use { fd ->
                extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            val track = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                return drain(extractor, codec, format, sampleRate)
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun drain(extractor: MediaExtractor, codec: MediaCodec, format: MediaFormat, targetRate: Int): ShortArray {
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 60_000_000L
        // Sized from the duration up front: a two-minute stereo loop is 11.5 M samples, so avoid growing by doubling.
        val pcm = ShortBuilder((durationUs * rate / 1_000_000 * 2 * 1.02).toInt())
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var idlePolls = 0
        while (idlePolls < MAX_IDLE_POLLS) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_US)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)!!
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                index >= 0 -> {
                    idlePolls = 0
                    val buffer = codec.getOutputBuffer(index)!!
                    buffer.position(info.offset)
                    buffer.limit(info.offset + info.size)
                    val shorts = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (shorts.remaining() >= channels) {
                        val l = shorts.get()
                        val r = if (channels > 1) shorts.get() else l
                        repeat(channels - 2) { shorts.get() }   // anything past stereo is dropped
                        pcm.add(l)
                        pcm.add(r)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
                else -> if (inputDone) idlePolls++
            }
        }
        val stereo = pcm.toArray()
        return if (rate == targetRate) stereo else resample(stereo, rate, targetRate)
    }

    /** Linear interpolation per channel of interleaved stereo; the shipped loops are already 48 kHz. */
    private fun resample(src: ShortArray, from: Int, to: Int): ShortArray {
        val inFrames = src.size / 2
        val outFrames = (inFrames.toLong() * to / from).toInt()
        val step = from.toDouble() / to
        val out = ShortArray(outFrames * 2)
        for (i in 0 until outFrames) {
            val x = i * step
            val j = x.toInt()
            val k = min(j + 1, inFrames - 1)
            val t = x - j
            for (c in 0..1) {
                val a = src[2 * j + c].toDouble()
                val b = src[2 * k + c].toDouble()
                out[2 * i + c] = (a + (b - a) * t).roundToInt().toShort()
            }
        }
        return out
    }

    private class ShortBuilder(initialCapacity: Int) {
        private var data = ShortArray(maxOf(initialCapacity, 1024))
        private var size = 0

        fun add(s: Short) {
            if (size == data.size) data = data.copyOf(data.size * 2)
            data[size++] = s
        }

        fun toArray(): ShortArray = data.copyOf(size)
    }
}
