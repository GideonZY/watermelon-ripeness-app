package com.example.watermelonripeness.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

object WavWriter {
    fun write(file: File, samples: ShortArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        BufferedOutputStream(FileOutputStream(file)).use { out ->
            val dataSize = samples.size * 2
            fun ascii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
            fun little16(value: Int) { out.write(value and 0xff); out.write(value shr 8 and 0xff) }
            fun little32(value: Int) {
                out.write(value and 0xff); out.write(value shr 8 and 0xff)
                out.write(value shr 16 and 0xff); out.write(value shr 24 and 0xff)
            }
            ascii("RIFF"); little32(36 + dataSize); ascii("WAVE")
            ascii("fmt "); little32(16); little16(1); little16(1)
            little32(sampleRate); little32(sampleRate * 2); little16(2); little16(16)
            ascii("data"); little32(dataSize)
            samples.forEach { little16(it.toInt()) }
        }
    }
}
