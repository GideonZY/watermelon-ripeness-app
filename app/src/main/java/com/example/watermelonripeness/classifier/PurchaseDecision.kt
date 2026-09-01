package com.example.watermelonripeness.classifier

enum class PurchaseDecision(val displayName: String) {
    RECOMMEND("推荐购买"),
    DO_NOT_BUY("不建议购买"),
    RETRY("建议重新检测")
}

enum class DetectionStability(val displayName: String) {
    STABLE("检测稳定"),
    UNSTABLE("结果不稳定"),
    INSUFFICIENT("有效拍击不足")
}

data class SessionClassification(
    val ripeness: Ripeness?,
    val purchaseDecision: PurchaseDecision,
    val stability: DetectionStability,
    val referenceFrequencyHz: Double?,
    val maturityIndex: Double?,
    val explanation: String
)
