package com.example.apneaapp
import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private var isRunning = false
    private lateinit var audioTrack: AudioTrack
    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 48000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val statusText = findViewById<TextView>(R.id.statusText)
        
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                isRunning = true
                statusText.text = "Sonar Active (18-20kHz)"
                startSonar()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            }
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            isRunning = false
            statusText.text = "Stopped"
        }
    }

    private fun startSonar() {
        Thread {
            val chirp = generateChirp()
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 8192)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(chirp.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            audioTrack.write(chirp, 0, chirp.size)
            audioTrack.setLoopPoints(0, chirp.size, -1)
            audioTrack.play()
            audioRecord.startRecording()
            while (isRunning) { audioRecord.read(ShortArray(8192), 0, 8192) }
            audioTrack.stop(); audioRecord.stop()
        }.start()
    }

    private fun generateChirp(): ShortArray {
        val numSamples = (sampleRate * 0.01075).toInt()
        val samples = ShortArray(numSamples)
        val k = (20000.0 - 18000.0) / 0.01075
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val phase = 2.0 * PI * (18000.0 * t + 0.5 * k * t * t)
            samples[i] = (sin(phase) * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }
}
