package com.codersergg.alertbridge.ktor

import com.codersergg.alertbridge.core.DefaultTelegramAlertClient
import com.codersergg.alertbridge.core.AlertQueueOverflowPolicy
import com.codersergg.alertbridge.core.ScopedTelegramAlertNotifier
import com.codersergg.alertbridge.core.TelegramAlertNotifier
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
    var serviceName: String = "application"
    var serviceVersion: String = "unknown"
    var dedupWindowSeconds: Long = 120
    var async: Boolean = true
    var queueCapacity: Int = 1024
    var overflowPolicy: AlertQueueOverflowPolicy = AlertQueueOverflowPolicy.DropOldest
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
            async = pluginConfig.async,
            queueCapacity = pluginConfig.queueCapacity,
            overflowPolicy = pluginConfig.overflowPolicy,
        )

    val notifier = ScopedTelegramAlertNotifier(
        service.scoped(serviceName = pluginConfig.serviceName, version = pluginConfig.serviceVersion)
    ) { result ->
        application.environment.log.warn("Telegram alert send failed: {}", result.reason ?: "unknown")
    }

    application.attributes.put(AlertBridgeServiceKey, service)
    application.attributes.put(AlertBridgeNotifierKey, notifier)
}

val AlertBridgeServiceKey = AttributeKey<TelegramAlertService>("alertbridge.service")
val AlertBridgeNotifierKey = AttributeKey<TelegramAlertNotifier>("alertbridge.notifier")

fun Application.alertBridge(): TelegramAlertService = attributes[AlertBridgeServiceKey]
fun Application.alertNotifier(): TelegramAlertNotifier = attributes[AlertBridgeNotifierKey]
