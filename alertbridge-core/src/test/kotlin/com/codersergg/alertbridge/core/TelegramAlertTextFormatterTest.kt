package com.codersergg.alertbridge.core

import kotlin.test.Test
import kotlin.test.assertTrue

class TelegramAlertTextFormatterTest {

    @Test
    fun `format adds emoji and bracket level in header`() {
        val text = TelegramAlertTextFormatter.format(
            TelegramAlertMessage(
                level = AlertLevel.Warning,
                title = "service:1.2.3",
                details = "event=test",
                fingerprint = "fp-1",
            )
        )

        assertTrue(text.startsWith("⚠️ [WARN] service:1.2.3\n"))
        assertTrue(text.contains("fingerprint=fp-1"))
    }
}
