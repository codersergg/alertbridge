package com.codersergg.alertbridge.spring

import com.codersergg.alertbridge.core.DefaultTelegramAlertClient
import com.codersergg.alertbridge.core.NoopTelegramAlertNotifier
import com.codersergg.alertbridge.core.ScopedTelegramAlertService
import com.codersergg.alertbridge.core.ScopedTelegramAlertNotifier
import com.codersergg.alertbridge.core.TelegramAlertNotifier
import com.codersergg.alertbridge.core.TelegramAlertClient
import com.codersergg.alertbridge.core.TelegramAlertConfig
import com.codersergg.alertbridge.core.TelegramAlertService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import java.time.Duration
import org.slf4j.LoggerFactory

@AutoConfiguration
@ConditionalOnClass(TelegramAlertService::class)
@EnableConfigurationProperties(AlertBridgeProperties::class)
class AlertBridgeAutoConfiguration {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "alertbridge.telegram", name = ["enabled"], havingValue = "true")
    fun telegramAlertConfig(props: AlertBridgeProperties): TelegramAlertConfig =
        TelegramAlertConfig(
            enabled = props.enabled,
            botToken = props.botToken,
            chatId = props.chatId,
            messageThreadId = props.messageThreadId,
            connectTimeout = Duration.ofMillis(props.connectTimeoutMs),
            requestTimeout = Duration.ofMillis(props.requestTimeoutMs),
            dedupWindow = Duration.ofSeconds(props.dedupWindowSeconds),
        )

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "alertbridge.telegram", name = ["enabled"], havingValue = "true")
    fun telegramAlertClient(config: TelegramAlertConfig): TelegramAlertClient =
        DefaultTelegramAlertClient(config)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "alertbridge.telegram", name = ["enabled"], havingValue = "true")
    fun telegramAlertService(
        client: TelegramAlertClient,
        props: AlertBridgeProperties,
    ): TelegramAlertService = TelegramAlertService(
        client = client,
        dedupWindowSeconds = props.dedupWindowSeconds,
        async = props.async,
        queueCapacity = props.queueCapacity,
        overflowPolicy = props.overflowPolicy,
    )

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "alertbridge.telegram", name = ["enabled"], havingValue = "true")
    fun scopedTelegramAlertService(
        alerts: TelegramAlertService,
        props: AlertBridgeProperties,
        buildPropertiesProvider: ObjectProvider<BuildProperties>,
        @Value("\${spring.application.name:application}") applicationName: String,
        @Value("\${APP_VERSION:}") appVersionFromEnv: String,
    ): ScopedTelegramAlertService {
        val serviceName = props.serviceName?.trim().orEmpty().ifBlank { applicationName }
        val buildVersion = buildPropertiesProvider.ifAvailable?.version?.trim().orEmpty()
        val serviceVersion = props.serviceVersion?.trim().orEmpty()
            .ifBlank { buildVersion }
            .ifBlank { appVersionFromEnv.trim() }
            .ifBlank { "unknown" }
        return alerts.scoped(serviceName = serviceName, version = serviceVersion)
    }

    @Bean
    @ConditionalOnMissingBean
    fun telegramAlertNotifier(
        scopedAlertsProvider: ObjectProvider<ScopedTelegramAlertService>,
    ): TelegramAlertNotifier {
        val scoped = scopedAlertsProvider.ifAvailable ?: return NoopTelegramAlertNotifier
        return ScopedTelegramAlertNotifier(scoped) { result ->
            log.warn("Telegram alert send failed: {}", result.reason ?: "unknown")
        }
    }
}
