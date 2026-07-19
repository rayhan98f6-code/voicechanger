package com.example.voicechanger

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.voicechanger.databinding.ActivityMainBinding
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sampleRate = 44100
    private var isRunning = false
    private var currentPreset = VoicePreset.NORMAL
    private var recordThread: Thread? = null

    private val minBufSize = AudioRecord.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startVoiceEngine() else
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPresetButtons()

        binding.btnStart.setOnClickListener {
            if (!isRunning) {
                checkPermissionAndStart()
            } else {
                stopVoiceEngine()
            }
        }
    }

    private fun setupPresetButtons() {
        val presetButtons = mapOf(
            binding.btnNormal to VoicePreset.NORMAL,
            binding.btnCuteGirl to VoicePreset.CUTE_GIRL,
            binding.btnDeepMale to VoicePreset.DEEP_MALE,
            binding.btnChipmunk to VoicePreset.CHIPMUNK,
            binding.btnRobot to VoicePreset.ROBOT,
            binding.btnMonster to VoicePreset.MONSTER
        )
        presetButtons.forEach { (button, preset) ->
            button.setOnClickListener {
                currentPreset = preset
                binding.tvCurrentVoice.text = "Current voice: ${preset.label}"
            }
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceEngine()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceEngine() {
        isRunning = true
        binding.btnStart.text = "Stop"

        recordThread = thread(start = true) {
            val bufSize = max(minBufSize, 2048)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize
            )

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val pitchShifter = PitchShifter(sampleRate)
            val buffer = ShortArray(bufSize / 2)

            audioRecord.startRecording()
            audioTrack.play()

            while (isRunning) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val input = buffer.copyOfRange(0, read)
                    val processed = pitchShifter.process(input, currentPreset.pitchFactor)
                    audioTrack.write(processed, 0, processed.size)
                }
            }

            audioRecord.stop()
            audioRecord.release()
            audioTrack.stop()
            audioTrack.release()
            pitchShifter.reset()
        }
    }

    private fun stopVoiceEngine() {
        isRunning = false
        binding.btnStart.text = "Start"
        recordThread?.join(500)
        recordThread = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVoiceEngine()
    }

    private fun max(a: Int, b: Int) = if (a > b) a else b
}
