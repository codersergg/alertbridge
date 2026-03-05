package com.codersergg.alertbridge.core

import java.util.concurrent.CompletableFuture

class ScopedTelegramAlertService(
    private val alerts: TelegramAlertService,
    private val serviceName: String,
    private val version: String,
) {
    @JvmOverloads
    fun info(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): TelegramAlertResult =
        infoRaw(details = buildDetails(event, fields), fingerprint = fingerprint)

    @JvmOverloads
    fun warning(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): TelegramAlertResult =
        warningRaw(details = buildDetails(event, fields), fingerprint = fingerprint)

    @JvmOverloads
    fun error(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): TelegramAlertResult =
        errorRaw(details = buildDetails(event, fields), fingerprint = fingerprint)

    @JvmOverloads
    fun critical(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): TelegramAlertResult =
        criticalRaw(details = buildDetails(event, fields), fingerprint = fingerprint)

    fun infoRaw(details: String, fingerprint: String): TelegramAlertResult =
        alerts.send(
            level = AlertLevel.Info,
            title = "$serviceName:$version",
            details = details,
            fingerprint = fingerprint,
            service = null,
        )

    fun warningRaw(details: String, fingerprint: String): TelegramAlertResult =
        alerts.send(
            level = AlertLevel.Warning,
            title = "$serviceName:$version",
            details = details,
            fingerprint = fingerprint,
            service = null,
        )

    fun errorRaw(details: String, fingerprint: String): TelegramAlertResult =
        alerts.send(
            level = AlertLevel.Error,
            title = "$serviceName:$version",
            details = details,
            fingerprint = fingerprint,
            service = null,
        )

    fun criticalRaw(details: String, fingerprint: String): TelegramAlertResult =
        alerts.send(
            level = AlertLevel.Critical,
            title = "$serviceName:$version",
            details = details,
            fingerprint = fingerprint,
            service = null,
        )

    @JvmOverloads
    fun infoAsync(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): CompletableFuture<TelegramAlertResult> =
        CompletableFuture.supplyAsync { info(event, fingerprint, fields) }

    @JvmOverloads
    fun warningAsync(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): CompletableFuture<TelegramAlertResult> =
        CompletableFuture.supplyAsync { warning(event, fingerprint, fields) }

    @JvmOverloads
    fun errorAsync(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): CompletableFuture<TelegramAlertResult> =
        CompletableFuture.supplyAsync { error(event, fingerprint, fields) }

    @JvmOverloads
    fun criticalAsync(event: String, fingerprint: String, fields: Map<String, Any?> = emptyMap()): CompletableFuture<TelegramAlertResult> =
        CompletableFuture.supplyAsync { critical(event, fingerprint, fields) }

    private fun buildDetails(event: String, fields: Map<String, Any?>): String {
        if (fields.isEmpty()) return "event=$event\n"
        val lines = buildList {
            add("event=$event")
            fields.forEach { (k, v) -> add("$k=${v ?: "null"}") }
        }
        return lines.joinToString("\n", postfix = "\n")
    }
}
