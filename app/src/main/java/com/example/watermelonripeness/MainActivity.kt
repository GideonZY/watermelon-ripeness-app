package com.example.watermelonripeness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.watermelonripeness.analysis.FeatureExtractor
import com.example.watermelonripeness.analysis.LiveFrequencyTracker
import com.example.watermelonripeness.audio.AudioRecorder
import com.example.watermelonripeness.classifier.RipenessClassifier
import com.example.watermelonripeness.classifier.RuleBasedClassifier
import com.example.watermelonripeness.ui.RipenessGaugeView
import com.google.android.material.button.MaterialButton
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var liveFrequencyText: TextView
    private lateinit var gaugeView: RipenessGaugeView
    private lateinit var liveDetectionCard: View
    private lateinit var resultCard: View
    private val executor = Executors.newSingleThreadExecutor()
    private val recorder = AudioRecorder()
    private val liveFrequencyTracker = LiveFrequencyTracker()
    private val classifier: RipenessClassifier = RuleBasedClassifier()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startRecording()
        } else {
            resultText.text = "检测结果：需要麦克风权限才能开始检测"
            applyUiPhase(DetectionPhase.COMPLETE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordButton = findViewById(R.id.recordButton)
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        liveFrequencyText = findViewById(R.id.liveFrequencyText)
        gaugeView = findViewById(R.id.ripenessGauge)
        liveDetectionCard = findViewById(R.id.liveDetectionCard)
        resultCard = findViewById(R.id.resultCard)
        applyUiPhase(DetectionPhase.IDLE)

        recordButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        recordButton.isEnabled = false
        recordButton.text = "检测中…"
        statusText.text = "实时检测中，请连续拍击西瓜 2～3 次"
        liveFrequencyText.text = "当前频率：等待有效拍击…"
        liveFrequencyTracker.reset()
        gaugeView.setGaugeValue(0f)
        applyUiPhase(DetectionPhase.DETECTING)

        executor.execute {
            try {
                val recording = recorder.record(
                    durationMs = RECORDING_DURATION_MS,
                    updateIntervalMs = LIVE_UPDATE_INTERVAL_MS
                ) { frame, sampleRate ->
                    val reading = liveFrequencyTracker.analyze(frame, sampleRate)
                    if (reading != null) {
                        runOnUiThread {
                            gaugeView.setGaugeValue(reading.gaugeValue)
                            liveFrequencyText.text = "当前频率：%.0f Hz".format(reading.displayFrequencyHz)
                        }
                    }
                }

                if (liveFrequencyTracker.validReadingCount == 0) {
                    runOnUiThread {
                        resultText.text = "检测结果：没有听到清晰拍击声，请换个安静环境再试一次"
                        applyUiPhase(DetectionPhase.COMPLETE)
                    }
                    return@execute
                }

                val features = FeatureExtractor.extract(recording.samples, recording.sampleRate)
                val result = classifier.classify(features, recording.samples, recording.sampleRate)
                runOnUiThread {
                    resultText.text = DetectionSummaryFormatter.format(result, features)
                    applyUiPhase(DetectionPhase.COMPLETE)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    resultText.text = "检测结果：本次检测失败，请稍后重试"
                    applyUiPhase(DetectionPhase.COMPLETE)
                }
            } finally {
                runOnUiThread {
                    recordButton.isEnabled = true
                    recordButton.text = "开始检测"
                }
            }
        }
    }

    private fun applyUiPhase(phase: DetectionPhase) {
        val visibility = DetectionUiState.forPhase(phase)
        liveDetectionCard.visibility = if (visibility.showLiveDetection) View.VISIBLE else View.GONE
        resultCard.visibility = if (visibility.showResult) View.VISIBLE else View.GONE
        statusText.visibility = if (visibility.showStatus) View.VISIBLE else View.GONE
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
