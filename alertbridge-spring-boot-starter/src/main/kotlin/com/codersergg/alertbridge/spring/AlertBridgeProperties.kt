package com.codersergg.alertbridge.spring

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "alertbridge.telegram")
data class AlertBridgeProperties(
    var enabled: Boolean = false,
    var botToken: String = "",
    var chatId: String = "",
    var messageThreadId: Long? = null,
    var connectTimeoutMs: Long = 2000,
    var requestTimeoutMs: Long = 3000,
    var dedupWindowSeconds: Long = 120,
    var serviceName: String? = null,
)
