package com.codersergg.alertbridge.core

import java.time.Duration

data class TelegramAlertConfig(
    val enabled: Boolean = true,
    val botToken: String,
    val chatId: String,
    val messageThreadId: Long? = null,
    val connectTimeout: Duration = Duration.ofSeconds(2),
    val requestTimeout: Duration = Duration.ofSeconds(3),
    val dedupWindow: Duration = Duration.ofMinutes(2),
)
