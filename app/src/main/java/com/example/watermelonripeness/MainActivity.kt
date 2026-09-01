package com.example.watermelonripeness

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.watermelonripeness.analysis.LiveFrequencyTracker
import com.example.watermelonripeness.analysis.TapFeatureExtractor
import com.example.watermelonripeness.analysis.TapSessionTracker
import com.example.watermelonripeness.audio.AudioRecorder
import com.example.watermelonripeness.classifier.DetectionStability
import com.example.watermelonripeness.classifier.LiteratureHeuristicClassifier
import com.example.watermelonripeness.classifier.PurchaseDecision
import com.example.watermelonripeness.classifier.RipenessScale
import com.example.watermelonripeness.classifier.SessionClassification
import com.example.watermelonripeness.ui.RipenessGaugeView
import com.google.android.material.button.MaterialButton
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var liveFrequencyText: TextView
    private lateinit var gaugeView: RipenessGaugeView
    private lateinit var instructionsCard: View
    private lateinit var liveDetectionCard: View
    private lateinit var resultCard: View
    private lateinit var resultHeadline: TextView
    private lateinit var resultRipeness: TextView
    private lateinit var resultStability: TextView
    private lateinit var resultExplanation: TextView
    private lateinit var resultReference: TextView

    private val executor = Executors.newSingleThreadExecutor()
    private val recorder = AudioRecorder()
    private val liveFrequencyTracker = LiveFrequencyTracker()
    private val tapSessionTracker = TapSessionTracker()
    private val classifier = LiteratureHeuristicClassifier()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startRecording()
        } else {
            renderResult(
                SessionClassification(
                    ripeness = null,
                    purchaseDecision = PurchaseDecision.RETRY,
                    stability = DetectionStability.INSUFFICIENT,
                    referenceFrequencyHz = null,
                    maturityIndex = null,
                    explanation = "需要允许麦克风权限，才能听到西瓜的拍击声。"
                )
            )
            applyUiPhase(DetectionPhase.COMPLETE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.recordButton)
        statusText = findViewById(R.id.statusText)
        liveFrequencyText = findViewById(R.id.liveFrequencyText)
        gaugeView = findViewById(R.id.ripenessGauge)
        instructionsCard = findViewById(R.id.instructionsCard)
        liveDetectionCard = findViewById(R.id.liveDetectionCard)
        resultCard = findViewById(R.id.resultCard)
        resultHeadline = findViewById(R.id.resultHeadline)
        resultRipeness = findViewById(R.id.resultRipeness)
        resultStability = findViewById(R.id.resultStability)
        resultExplanation = findViewById(R.id.resultExplanation)
        resultReference = findViewById(R.id.resultReference)

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
        statusText.text = "正在检测，请连续轻拍西瓜中部"
        liveFrequencyText.text = "正在听…"
        liveFrequencyTracker.reset()
        tapSessionTracker.reset()
        gaugeView.setGaugeValue(0f)
        applyUiPhase(DetectionPhase.DETECTING)

        executor.execute {
            try {
                val recording = recorder.recordUntil(
                    durationMs = MAX_RECORDING_DURATION_MS,
                    updateIntervalMs = LIVE_UPDATE_INTERVAL_MS
                ) { frame, sampleRate, frameStartSample ->
                    val reading = liveFrequencyTracker.analyze(frame, sampleRate)
                    val tapUpdate = tapSessionTracker.processFrame(frame, sampleRate, frameStartSample)

                    if (reading != null || tapUpdate.detectedTap) {
                        runOnUiThread {
                            reading?.let {
                                val shownFrequency = if (tapUpdate.detectedTap) it.rawFrequencyHz else it.displayFrequencyHz
                                val shownGauge = if (tapUpdate.detectedTap) RipenessScale.gaugeValue(it.rawFrequencyHz) else it.gaugeValue
                                gaugeView.setGaugeValue(shownGauge)
                                liveFrequencyText.text = "当前频率：%.0f Hz".format(shownFrequency)
                            }
                            if (tapUpdate.detectedTap) gaugeView.pulseTap()
                        }
                    }

                    tapSessionTracker.shouldStopAfterFrame()
                }

                val tapFeatures = tapSessionTracker.tapPeakSamples
                    .take(LiteratureHeuristicClassifier.REQUIRED_TAPS)
                    .mapNotNull { peakSample ->
                        if (peakSample !in recording.samples.indices) null
                        else runCatching {
                            TapFeatureExtractor.extract(recording.samples, recording.sampleRate, peakSample)
                        }.getOrNull()
                    }

                val result = classifier.classify(tapFeatures)
                runOnUiThread {
                    renderResult(result)
                    applyUiPhase(DetectionPhase.COMPLETE)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    renderResult(
                        SessionClassification(
                            ripeness = null,
                            purchaseDecision = PurchaseDecision.RETRY,
                            stability = DetectionStability.UNSTABLE,
                            referenceFrequencyHz = null,
                            maturityIndex = null,
                            explanation = "本次检测没有完成，请重新试一次。"
                        )
                    )
                    applyUiPhase(DetectionPhase.COMPLETE)
                }
            } finally {
                runOnUiThread { recordButton.isEnabled = true }
            }
        }
    }

    private fun renderResult(result: SessionClassification) {
        val ui = DetectionSummaryFormatter.format(result)
        resultHeadline.text = ui.headline
        resultRipeness.text = ui.ripenessLabel
        resultRipeness.visibility = if (ui.ripenessLabel.isBlank()) View.GONE else View.VISIBLE
        resultStability.text = ui.stabilityLabel
        resultExplanation.text = ui.explanation
        resultReference.text = ui.referenceText
        resultReference.visibility = if (ui.referenceText.isBlank()) View.GONE else View.VISIBLE

        val headlineColor = when (result.purchaseDecision) {
            PurchaseDecision.RECOMMEND -> R.color.result_recommend
            PurchaseDecision.DO_NOT_BUY -> R.color.result_avoid
            PurchaseDecision.RETRY -> R.color.result_retry
        }
        resultHeadline.setTextColor(ContextCompat.getColor(this, headlineColor))
        recordButton.text = if (result.purchaseDecision == PurchaseDecision.RETRY) "重新检测" else "再测一次"
    }

    private fun applyUiPhase(phase: DetectionPhase) {
        val visibility = DetectionUiState.forPhase(phase)
        instructionsCard.visibility = if (visibility.showInstructions) View.VISIBLE else View.GONE
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
        private const val MAX_RECORDING_DURATION_MS = 5_000
    }
}
