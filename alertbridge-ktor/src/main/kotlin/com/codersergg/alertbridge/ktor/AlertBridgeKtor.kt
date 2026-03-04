package com.codersergg.alertbridge.ktor

import com.codersergg.alertbridge.core.DefaultTelegramAlertClient
import com.codersergg.alertbridge.core.TelegramAlertClient
import com.codersergg.alertbridge.core.TelegramAlertConfig
import com.codersergg.alertbridge.core.TelegramAlertService
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey

class AlertBridgeKtorConfig {
    var service: TelegramAlertService? = null
    var client: TelegramAlertClient? = null
    var config: TelegramAlertConfig? = null
    var dedupWindowSeconds: Long = 120
}

val AlertBridgeKtorPlugin = createApplicationPlugin(
    name = "AlertBridgeKtor",
    createConfiguration = ::AlertBridgeKtorConfig,
) {
    val service = pluginConfig.service
        ?: TelegramAlertService(
            client = pluginConfig.client ?: DefaultTelegramAlertClient(
                pluginConfig.config ?: error("AlertBridgeKtor: provide service, client, or config")
            ),
            dedupWindowSeconds = pluginConfig.dedupWindowSeconds,
        )

    application.attributes.put(AlertBridgeServiceKey, service)
}

val AlertBridgeServiceKey = AttributeKey<TelegramAlertService>("alertbridge.service")

fun Application.alertBridge(): TelegramAlertService = attributes[AlertBridgeServiceKey]
