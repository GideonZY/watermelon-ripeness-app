package com.example.watermelonripeness.classifier

import com.example.watermelonripeness.analysis.AudioFeatures

/**
 * 后续模型替换点：保持此接口不变，在这里加载 assets/watermelon_model.tflite，
 * 将 pcm 转成模型需要的波形或 log-mel 频谱，再映射三个输出类别。
 */
class TFLiteRipenessClassifier : RipenessClassifier {
    override fun classify(features: AudioFeatures, pcm: ShortArray, sampleRate: Int): Classification {
        error("尚未放入训练好的 TensorFlow Lite 模型")
    }
}
