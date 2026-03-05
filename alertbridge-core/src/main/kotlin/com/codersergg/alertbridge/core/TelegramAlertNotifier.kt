package com.codersergg.alertbridge.core

interface TelegramAlertNotifier {
    fun notifyWarning(details: String, fingerprint: String)

    fun notifyInfo(details: String, fingerprint: String)

    fun notifyCritical(details: String, fingerprint: String)
}

class ScopedTelegramAlertNotifier(
    private val scopedAlerts: ScopedTelegramAlertService,
    private val onFailure: (TelegramAlertResult) -> Unit = {},
) : TelegramAlertNotifier {
    override fun notifyWarning(details: String, fingerprint: String) {
        send { scopedAlerts.warningRaw(details = details, fingerprint = fingerprint) }
    }

    override fun notifyInfo(details: String, fingerprint: String) {
        send { scopedAlerts.infoRaw(details = details, fingerprint = fingerprint) }
    }

    override fun notifyCritical(details: String, fingerprint: String) {
        send { scopedAlerts.criticalRaw(details = details, fingerprint = fingerprint) }
    }

    private fun send(action: () -> TelegramAlertResult) {
        val result = action()
        if (!result.sent && result.reason != "dedup_suppressed") {
            onFailure(result)
        }
    }
}

object NoopTelegramAlertNotifier : TelegramAlertNotifier {
    override fun notifyWarning(details: String, fingerprint: String) = Unit

    override fun notifyInfo(details: String, fingerprint: String) = Unit

    override fun notifyCritical(details: String, fingerprint: String) = Unit
}
