package com.example.voicechanger

import kotlin.math.max
import kotlin.math.min

/**
 * Simple real-time pitch shifter using time-domain overlap-add (OLA).
 *
 * This is a lightweight, pure-Kotlin implementation good enough for an MVP.
 * For production-quality, natural-sounding voices (especially "cute girl" /
 * anime style with formant correction), replace this with the SoundTouch
 * library via NDK (see README.md in this project for instructions).
 *
 * pitchFactor:
 *   1.0f  = no change
 *   >1.0f = higher pitch (e.g. 1.4f for "cute girl")
 *   <1.0f = lower pitch (e.g. 0.75f for "deep male")
 */
class PitchShifter(private val sampleRate: Int) {

    private val frameSize = 1024
    private val overlap = 0.5f
    private val hopSize = (frameSize * (1 - overlap)).toInt()

    // Circular input buffer for accumulating samples between calls
    private var inputBuffer = ShortArray(0)

    fun process(input: ShortArray, pitchFactor: Float): ShortArray {
        if (pitchFactor == 1.0f) return input

        // Append new input to leftover buffer
        val combined = ShortArray(inputBuffer.size + input.size)
        System.arraycopy(inputBuffer, 0, combined, 0, inputBuffer.size)
        System.arraycopy(input, 0, combined, inputBuffer.size, input.size)

        val output = ArrayList<Short>(combined.size)
        var pos = 0

        while (pos + frameSize <= combined.size) {
            val frame = combined.copyOfRange(pos, pos + frameSize)
            val resampled = resample(frame, pitchFactor)
            for (s in resampled) output.add(s)
            pos += hopSize
        }

        // Keep leftover samples for next call
        inputBuffer = if (pos < combined.size) {
            combined.copyOfRange(pos, combined.size)
        } else {
            ShortArray(0)
        }

        return output.toShortArray()
    }

    /**
     * Resample a frame to shift pitch. Reading the frame at a different
     * rate changes both pitch and duration; combined with overlap-add
     * hop timing this approximates a pitch shift while keeping the
     * output stream roughly real-time.
     */
    private fun resample(frame: ShortArray, factor: Float): ShortArray {
        val outLen = (frame.size / factor).toInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i * factor
            val idx = srcPos.toInt()
            val frac = srcPos - idx
            val s0 = frame[min(idx, frame.size - 1)]
            val s1 = frame[min(idx + 1, frame.size - 1)]
            out[i] = (s0 + (s1 - s0) * frac).toInt().toShort()
        }
        return out
    }

    fun reset() {
        inputBuffer = ShortArray(0)
    }
}

/** Preset voice definitions used by the UI */
enum class VoicePreset(val label: String, val pitchFactor: Float) {
    NORMAL("Normal", 1.0f),
    CUTE_GIRL("Cute Girl", 1.45f),
    DEEP_MALE("Deep Male", 0.72f),
    CHIPMUNK("Chipmunk", 1.8f),
    ROBOT("Robot", 0.95f),   // combine with a ring-mod effect for full robot sound
    MONSTER("Monster", 0.6f)
}
