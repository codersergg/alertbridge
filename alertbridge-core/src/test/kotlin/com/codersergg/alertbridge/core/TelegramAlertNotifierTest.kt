package com.codersergg.alertbridge.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelegramAlertNotifierTest {

    @Test
    fun `scoped notifier invokes failure callback for non-dedup failures`() {
        val scoped = TelegramAlertService(
            client = TelegramAlertClient {
                TelegramAlertResult(sent = false, reason = "network_error")
            },
            async = false,
            dedupWindowSeconds = 0,
        ).scoped("svc", "v1")
        var callbackCalls = 0
        val notifier = ScopedTelegramAlertNotifier(scoped) { callbackCalls++ }

        notifier.notifyWarning(details = "warn", fingerprint = "fp-1")

        assertEquals(1, callbackCalls)
    }

    @Test
    fun `scoped notifier ignores dedup suppression`() {
        val scoped = TelegramAlertService(
            client = TelegramAlertClient {
                TelegramAlertResult(sent = false, reason = "network_error")
            },
            async = false,
            dedupWindowSeconds = 120,
        ).scoped("svc", "v1")
        var callbackCalls = 0
        val notifier = ScopedTelegramAlertNotifier(scoped) { callbackCalls++ }

        notifier.notifyInfo(details = "info", fingerprint = "same-fingerprint")
        notifier.notifyInfo(details = "info", fingerprint = "same-fingerprint")

        assertEquals(1, callbackCalls)
    }

    @Test
    fun `noop notifier does nothing`() {
        NoopTelegramAlertNotifier.notifyCritical(details = "critical", fingerprint = "fp-3")
        assertTrue(true)
    }
}
