package com.codersergg.alertbridge.core

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class TelegramAlertService(
    private val client: TelegramAlertClient,
    private val dedupWindowSeconds: Long = 120,
) {
    private val lastSentEpochByFingerprint = ConcurrentHashMap<String, Long>()

    fun info(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Info, title, details, fingerprint, service)

    fun warning(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Warning, title, details, fingerprint, service)

    fun error(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Error, title, details, fingerprint, service)

    fun critical(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Critical, title, details, fingerprint, service)

    fun send(level: AlertLevel, title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult {
        if (!shouldSend(fingerprint)) {
            return TelegramAlertResult(sent = false, reason = "dedup_suppressed")
        }

        return client.send(
            TelegramAlertMessage(
                level = level,
                title = title,
                details = details,
                fingerprint = fingerprint,
                at = Instant.now(),
                service = service,
            )
        )
    }

    private fun shouldSend(fingerprint: String): Boolean {
        val now = Instant.now().epochSecond
        val previous = lastSentEpochByFingerprint.put(fingerprint, now)
        return previous == null || (now - previous) >= dedupWindowSeconds
    }
}
