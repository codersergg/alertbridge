package com.codersergg.alertbridge.spring

import com.codersergg.alertbridge.core.DefaultTelegramAlertClient
import com.codersergg.alertbridge.core.TelegramAlertClient
import com.codersergg.alertbridge.core.TelegramAlertConfig
import com.codersergg.alertbridge.core.TelegramAlertService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.time.Duration

@AutoConfiguration
@ConditionalOnClass(TelegramAlertService::class)
@EnableConfigurationProperties(AlertBridgeProperties::class)
class AlertBridgeAutoConfiguration {

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
    )
}
