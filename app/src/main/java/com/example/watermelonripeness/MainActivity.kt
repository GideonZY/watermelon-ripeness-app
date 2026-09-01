package com.example.watermelonripeness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.watermelonripeness.analysis.FeatureExtractor
import com.example.watermelonripeness.audio.AudioRecorder
import com.example.watermelonripeness.classifier.RipenessClassifier
import com.example.watermelonripeness.classifier.RuleBasedClassifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private val executor = Executors.newSingleThreadExecutor()
    private val recorder = AudioRecorder()
    private val classifier: RipenessClassifier = RuleBasedClassifier()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else statusText.text = "需要麦克风权限才能检测"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordButton = findViewById(R.id.recordButton)
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        recordButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        recordButton.isEnabled = false
        statusText.text = "正在录音 2.5 秒…请拍击西瓜 2～3 次"
        resultText.text = ""
        val folder = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val output = File(folder, "watermelon_$stamp.wav")
        executor.execute {
            try {
                val recording = recorder.record(2500, output)
                val features = FeatureExtractor.extract(recording.samples, recording.sampleRate)
                val result = classifier.classify(features, recording.samples, recording.sampleRate)
                runOnUiThread {
                    statusText.text = "检测完成 · WAV 已保存\n${output.absolutePath}"
                    resultText.text = "${result.ripeness.displayName}\n\n${result.explanation}\n" +
                        "主频 %.0f Hz · 频谱质心 %.0f Hz\n能量 %.4f · 衰减 %.1f dB".format(
                            features.dominantFrequencyHz, features.spectralCentroidHz,
                            features.rms, features.decayRatio
                        )
                }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "检测失败：${e.message}" }
            } finally {
                runOnUiThread { recordButton.isEnabled = true }
            }
        }
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
