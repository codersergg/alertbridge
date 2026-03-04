package com.codersergg.alertbridge.core

import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramAlertServiceTest {

    @Test
    fun `dedup suppresses duplicate fingerprints`() {
        val sent = mutableListOf<TelegramAlertMessage>()
        val client = TelegramAlertClient { message ->
            sent += message
            TelegramAlertResult(sent = true)
        }
        val service = TelegramAlertService(client, dedupWindowSeconds = 60)

        val first = service.warning("title", "details", "fp-1")
        val second = service.warning("title", "details", "fp-1")

        assertEquals(true, first.sent)
        assertEquals(false, second.sent)
        assertEquals("dedup_suppressed", second.reason)
        assertEquals(1, sent.size)
    }
}
