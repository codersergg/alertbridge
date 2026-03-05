package com.codersergg.alertbridge.core

object TelegramAlertTextFormatter {
    fun format(message: TelegramAlertMessage): String {
        val details = message.details.trimEnd()
        val tags = if (message.tags.isEmpty()) "" else message.tags.entries.joinToString(" ") { (k, v) -> "$k=$v" }
        val servicePart = message.service?.let { "service=$it\n" }.orEmpty()
        return "${message.level.emoji()} [${message.level.label()}] ${message.title}\n" +
            servicePart +
            details + "\n" +
            "fingerprint=${message.fingerprint}\n" +
            "at=${message.at}" +
            if (tags.isBlank()) "\n" else "\n$tags\n"
    }
}
