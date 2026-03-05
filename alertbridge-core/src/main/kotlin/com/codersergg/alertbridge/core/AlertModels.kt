package com.codersergg.alertbridge.core

import java.time.Instant

enum class AlertLevel {
    Info,
    Warning,
    Error,
    Critical,
}

enum class AlertQueueOverflowPolicy {
    DropOldest,
    DropNew,
}

fun AlertLevel.label(): String = when (this) {
    AlertLevel.Info -> "INFO"
    AlertLevel.Warning -> "WARN"
    AlertLevel.Error -> "ERROR"
    AlertLevel.Critical -> "CRITICAL"
}

fun AlertLevel.emoji(): String = when (this) {
    AlertLevel.Info -> "ℹ️"
    AlertLevel.Warning -> "⚠️"
    AlertLevel.Error -> "❌"
    AlertLevel.Critical -> "🚨"
}

data class TelegramAlertMessage(
    val level: AlertLevel,
    val title: String,
    val details: String,
    val fingerprint: String,
    val at: Instant = Instant.now(),
    val service: String? = null,
    val tags: Map<String, String> = emptyMap(),
)

data class TelegramAlertResult(
    val sent: Boolean,
    val reason: String? = null,
)
