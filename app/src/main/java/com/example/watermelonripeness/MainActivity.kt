package com.example.watermelonripeness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.watermelonripeness.analysis.FeatureExtractor
import com.example.watermelonripeness.audio.AudioRecorder
import com.example.watermelonripeness.classifier.RipenessClassifier
import com.example.watermelonripeness.classifier.RipenessScale
import com.example.watermelonripeness.classifier.RuleBasedClassifier
import com.example.watermelonripeness.ui.RipenessGaugeView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var gaugeView: RipenessGaugeView
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
        gaugeView = findViewById(R.id.ripenessGauge)
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
        gaugeView.visibility = View.INVISIBLE
        executor.execute {
            try {
                // v0.2.0：录音只保留在内存中，不生成 WAV，不写入手机存储。
                val recording = recorder.record(2500)
                val features = FeatureExtractor.extract(recording.samples, recording.sampleRate)
                val result = classifier.classify(features, recording.samples, recording.sampleRate)
                val gaugeValue = RipenessScale.gaugeValue(features.dominantFrequencyHz)
                runOnUiThread {
                    statusText.text = "检测完成 · 本次录音未保存"
                    gaugeView.setGaugeValue(gaugeValue)
                    gaugeView.visibility = View.VISIBLE
                    resultText.text = DetectionSummaryFormatter.format(result, features)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "检测失败：${e.message}"
                    gaugeView.visibility = View.INVISIBLE
                }
            } finally {
                runOnUiThread { recordButton.isEnabled = true }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
