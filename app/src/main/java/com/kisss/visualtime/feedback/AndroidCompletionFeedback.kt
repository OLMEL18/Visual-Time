package com.kisss.visualtime.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AndroidCompletionFeedback(context: Context) : CompletionFeedback {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun play() {
        playGeneratedDoubleChime()
        playOptionalHaptic()
        Log.i(LogTag, "Foreground completion feedback emitted")
    }

    private fun playGeneratedDoubleChime() {
        runCatching {
            val samples = generateDoubleChime()
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SampleRateHertz)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = audioTrack.write(samples, 0, samples.size)
            if (written <= 0) {
                audioTrack.release()
                return
            }
            audioTrack.setVolume(ChimeVolume)
            audioTrack.play()
            mainHandler.postDelayed(
                {
                    runCatching { audioTrack.stop() }
                    audioTrack.release()
                },
                ReleaseDelayMillis,
            )
        }.onFailure { error ->
            Log.w(LogTag, "Unable to play completion chime", error)
        }
    }

    private fun playOptionalHaptic() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0L, 45L, 90L, 45L),
                intArrayOf(0, 70, 0, 45),
                -1,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }.onFailure { error ->
            Log.w(LogTag, "Unable to play completion haptic", error)
        }
    }

    private fun generateDoubleChime(): ShortArray {
        val sampleCount = (ChimeDurationSeconds * SampleRateHertz).toInt()
        return ShortArray(sampleCount) { index ->
            val timeSeconds = index.toDouble() / SampleRateHertz
            val first = decayingTone(timeSeconds - FirstChimeStartSeconds, 880.0)
            val second = decayingTone(timeSeconds - SecondChimeStartSeconds, 1_174.66)
            ((first + second).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun decayingTone(localSeconds: Double, frequencyHertz: Double): Double {
        if (localSeconds !in 0.0..ToneDurationSeconds) return 0.0
        val attack = (localSeconds / AttackSeconds).coerceIn(0.0, 1.0)
        val decay = exp(-7.0 * localSeconds)
        return sin(2.0 * PI * frequencyHertz * localSeconds) * attack * decay * 0.32
    }

    private companion object {
        const val LogTag = "VisualTimeCompletion"
        const val SampleRateHertz = 44_100
        const val ChimeDurationSeconds = 0.95
        const val FirstChimeStartSeconds = 0.04
        const val SecondChimeStartSeconds = 0.43
        const val ToneDurationSeconds = 0.42
        const val AttackSeconds = 0.018
        const val ChimeVolume = 0.48f
        const val ReleaseDelayMillis = 1_100L
    }
}
