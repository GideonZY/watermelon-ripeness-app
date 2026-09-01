package com.example.watermelonripeness.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import kotlin.math.min

data class Recording(val samples: ShortArray, val sampleRate: Int, val wavFile: File)

class AudioRecorder(private val sampleRate: Int = 16_000) {
    @SuppressLint("MissingPermission")
    fun record(durationMs: Int, output: File): Recording {
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minimum = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        require(minimum > 0) { "设备不支持当前录音参数" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            sampleRate,
            channel,
            encoding,
            maxOf(minimum, 2048)
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "麦克风初始化失败" }

        val wanted = sampleRate * durationMs / 1000
        val samples = ShortArray(wanted)
        var offset = 0
        try {
            recorder.startRecording()
            while (offset < wanted) {
                val count = recorder.read(samples, offset, min(2048, wanted - offset))
                if (count < 0) error("录音读取失败：$count")
                offset += count
            }
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            recorder.release()
        }
        WavWriter.write(output, samples, sampleRate)
        return Recording(samples, sampleRate, output)
    }
}
