package com.codersergg.alertbridge.core

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class DefaultTelegramAlertClient(
    private val config: TelegramAlertConfig,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(config.connectTimeout)
        .build(),
) : TelegramAlertClient {

    override fun send(message: TelegramAlertMessage): TelegramAlertResult {
        if (!config.enabled) return TelegramAlertResult(sent = false, reason = "disabled")

        val text = TelegramAlertTextFormatter.format(message)
        val body = buildFormBody(text)
        val endpoint = "https://api.telegram.org/bot${config.botToken}/sendMessage"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(config.requestTimeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return runCatching {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                TelegramAlertResult(sent = true)
            } else {
                TelegramAlertResult(sent = false, reason = "telegram_status_${response.statusCode()}")
            }
        }.getOrElse { ex ->
            TelegramAlertResult(sent = false, reason = ex.message ?: "request_failed")
        }
    }

    private fun buildFormBody(text: String): String {
        val fields = mutableListOf(
            "chat_id" to config.chatId,
            "text" to text,
            "disable_web_page_preview" to "true",
        )
        config.messageThreadId?.let { fields.add("message_thread_id" to it.toString()) }

        return fields.joinToString("&") { (k, v) ->
            "${encode(k)}=${encode(v)}"
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
