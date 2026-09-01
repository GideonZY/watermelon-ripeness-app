package com.example.watermelonripeness.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.min

data class Recording(val samples: ShortArray, val sampleRate: Int)

class AudioRecorder(private val sampleRate: Int = 16_000) {
    fun record(durationMs: Int): Recording = record(durationMs, durationMs) { _, _ -> }

    /**
     * 连续录音并按固定周期把最新 PCM 帧回调给分析层。
     * 音频始终只存在内存中，不生成 WAV，也不写入手机存储。
     */
    @SuppressLint("MissingPermission")
    fun record(
        durationMs: Int,
        updateIntervalMs: Int,
        onFrame: (ShortArray, Int) -> Unit
    ): Recording = recordUntil(durationMs, updateIntervalMs) { frame, rate, _ ->
        onFrame(frame, rate)
        false
    }

    /**
     * 与 [record] 相同，但回调可返回 true 提前结束录音。
     * frameStartSample 是当前帧在整段 PCM 中的起始采样点。
     */
    @SuppressLint("MissingPermission")
    fun recordUntil(
        durationMs: Int,
        updateIntervalMs: Int,
        onFrame: (ShortArray, Int, Int) -> Boolean
    ): Recording {
        require(durationMs > 0) { "录音时长必须大于 0" }
        require(updateIntervalMs > 0) { "刷新周期必须大于 0" }

        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val frameSamples = (sampleRate * updateIntervalMs / 1000).coerceAtLeast(1)
        val minimum = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        require(minimum > 0) { "设备不支持当前录音参数" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            sampleRate,
            channel,
            encoding,
            maxOf(minimum, frameSamples * 2)
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "麦克风初始化失败" }

        val wanted = sampleRate * durationMs / 1000
        val samples = ShortArray(wanted)
        var offset = 0

        try {
            recorder.startRecording()
            while (offset < wanted) {
                val currentFrameSize = min(frameSamples, wanted - offset)
                val frame = ShortArray(currentFrameSize)
                var frameOffset = 0

                while (frameOffset < currentFrameSize) {
                    val count = recorder.read(frame, frameOffset, currentFrameSize - frameOffset)
                    if (count < 0) error("录音读取失败：$count")
                    if (count == 0) continue
                    frameOffset += count
                }

                val frameStartSample = offset
                frame.copyInto(samples, destinationOffset = offset)
                offset += currentFrameSize
                if (onFrame(frame, sampleRate, frameStartSample)) break
            }
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            recorder.release()
        }

        return Recording(samples.copyOf(offset), sampleRate)
    }
}
