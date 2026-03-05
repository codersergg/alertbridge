package com.codersergg.alertbridge.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ScopedTelegramAlertServiceTest {

    @Test
    fun `scoped service uses service and version as title`() {
        val sent = mutableListOf<TelegramAlertMessage>()
        val client = TelegramAlertClient { message ->
            sent += message
            TelegramAlertResult(sent = true)
        }
        val service = TelegramAlertService(client, dedupWindowSeconds = 60, async = false)
        val scoped = service.scoped("lingflow-backend-test", "0.25.0-rc.21")

        scoped.info("event=application_started", "startup-1")

        assertEquals(1, sent.size)
        assertEquals("lingflow-backend-test:0.25.0-rc.21", sent.first().title)
        assertEquals(AlertLevel.Info, sent.first().level)
    }
}
