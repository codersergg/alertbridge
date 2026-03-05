package com.codersergg.alertbridge.core

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class TelegramAlertService(
    private val client: TelegramAlertClient,
    private val dedupWindowSeconds: Long = 120,
    private val async: Boolean = true,
    queueCapacity: Int = 1024,
    overflowPolicy: AlertQueueOverflowPolicy = AlertQueueOverflowPolicy.DropOldest,
) {
    private val lastSentEpochByFingerprint = ConcurrentHashMap<String, Long>()
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendQueue: Channel<TelegramAlertMessage>? = if (async) {
        Channel<TelegramAlertMessage>(
            capacity = queueCapacity.coerceAtLeast(1),
            onBufferOverflow = when (overflowPolicy) {
                AlertQueueOverflowPolicy.DropOldest -> BufferOverflow.DROP_OLDEST
                AlertQueueOverflowPolicy.DropNew -> BufferOverflow.DROP_LATEST
            }
        ).also { queue: Channel<TelegramAlertMessage> ->
            workerScope.launch {
                for (message in queue) {
                    client.send(message)
                }
            }
        }
    } else {
        null
    }

    fun info(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Info, title, details, fingerprint, service)

    fun warning(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Warning, title, details, fingerprint, service)

    fun error(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Error, title, details, fingerprint, service)

    fun critical(title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult =
        send(AlertLevel.Critical, title, details, fingerprint, service)

    fun scoped(serviceName: String, version: String): ScopedTelegramAlertService =
        ScopedTelegramAlertService(this, serviceName, version)

    fun send(level: AlertLevel, title: String, details: String, fingerprint: String, service: String? = null): TelegramAlertResult {
        if (!shouldSend(fingerprint)) {
            return TelegramAlertResult(sent = false, reason = "dedup_suppressed")
        }

        val message = TelegramAlertMessage(
            level = level,
            title = title,
            details = details,
            fingerprint = fingerprint,
            at = Instant.now(),
            service = service,
        )
        if (!async) {
            return client.send(message)
        }
        val queued = sendQueue?.trySend(message)?.isSuccess == true
        return if (queued) {
            TelegramAlertResult(sent = true, reason = "queued")
        } else {
            TelegramAlertResult(sent = false, reason = "queue_full_dropped_new")
        }
    }

    private fun shouldSend(fingerprint: String): Boolean {
        val now = Instant.now().epochSecond
        val previous = lastSentEpochByFingerprint.put(fingerprint, now)
        return previous == null || (now - previous) >= dedupWindowSeconds
    }
}
