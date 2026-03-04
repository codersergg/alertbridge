package com.codersergg.alertbridge.core

fun interface TelegramAlertClient {
    fun send(message: TelegramAlertMessage): TelegramAlertResult
}
