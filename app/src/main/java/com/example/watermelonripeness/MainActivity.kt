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
import com.example.watermelonripeness.analysis.LiveFrequencyTracker
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
    private lateinit var liveFrequencyText: TextView
    private lateinit var gaugeView: RipenessGaugeView
    private val executor = Executors.newSingleThreadExecutor()
    private val recorder = AudioRecorder()
    private val liveFrequencyTracker = LiveFrequencyTracker()
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
        liveFrequencyText = findViewById(R.id.liveFrequencyText)
        gaugeView = findViewById(R.id.ripenessGauge)
        recordButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        recordButton.isEnabled = false
        recordButton.text = "检测中…"
        statusText.text = "实时检测中 · 请拍击西瓜 2～3 次"
        resultText.text = ""
        liveFrequencyText.text = "当前主频：等待有效拍击…"
        liveFrequencyTracker.reset()
        gaugeView.setGaugeValue(0f)
        gaugeView.visibility = View.VISIBLE
        liveFrequencyText.visibility = View.VISIBLE

        executor.execute {
            try {
                // v0.2.1：100 ms 一帧实时分析；完整录音仍只保留在内存中。
                val recording = recorder.record(
                    durationMs = RECORDING_DURATION_MS,
                    updateIntervalMs = LIVE_UPDATE_INTERVAL_MS
                ) { frame, sampleRate ->
                    val reading = liveFrequencyTracker.analyze(frame, sampleRate)
                    if (reading != null) {
                        runOnUiThread {
                            gaugeView.setGaugeValue(reading.gaugeValue)
                            liveFrequencyText.text = "当前主频：%.0f Hz".format(reading.displayFrequencyHz)
                        }
                    }
                }

                if (liveFrequencyTracker.validReadingCount == 0) {
                    runOnUiThread {
                        statusText.text = "未检测到有效拍击，请重试"
                        liveFrequencyText.text = "当前主频：-- Hz"
                        resultText.text = ""
                    }
                    return@execute
                }

                val features = FeatureExtractor.extract(recording.samples, recording.sampleRate)
                val result = classifier.classify(features, recording.samples, recording.sampleRate)
                val finalGaugeValue = RipenessScale.gaugeValue(features.dominantFrequencyHz)
                runOnUiThread {
                    statusText.text = "检测完成 · 本次录音未保存"
                    gaugeView.setGaugeValue(finalGaugeValue)
                    liveFrequencyText.text = "最终主频：%.0f Hz".format(features.dominantFrequencyHz)
                    resultText.text = DetectionSummaryFormatter.format(result, features)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "检测失败：${e.message}"
                    liveFrequencyText.text = "当前主频：-- Hz"
                }
            } finally {
                runOnUiThread {
                    recordButton.isEnabled = true
                    recordButton.text = "开始检测"
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val LIVE_UPDATE_INTERVAL_MS = 100
        private const val RECORDING_DURATION_MS = 2500
    }
}
