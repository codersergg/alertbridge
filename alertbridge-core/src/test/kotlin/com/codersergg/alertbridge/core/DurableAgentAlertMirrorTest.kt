package com.codersergg.alertbridge.core

import java.net.URI
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DurableAgentAlertMirrorTest {
    @Test
    fun `shared HTTPS ingress path admits durable event`() {
        val directory = Files.createTempDirectory("alert-shared-path")
        try {
            DurableAgentAlertMirror(URI("https://alerts.example/_agent/alerts"), "backend-prod", "1.9.1",
                directory.resolve("key"), directory, deliver = { false }).use { mirror ->
                assertTrue(mirror.enqueue(TelegramAlertMessage(AlertLevel.Error, "failure", "", "fp")))
                mirror.flush()
                assertEquals(1L, Files.list(directory).use { it.count() })
            }
        } finally {
            Files.list(directory).use { it.forEach(Files::deleteIfExists) }
            Files.delete(directory)
        }
    }

    @Test
    fun `telegram dedup does not suppress durable agent admission`() {
        val directory = Files.createTempDirectory("alert-dedup")
        try {
            DurableAgentAlertMirror(URI("https://alerts.example/alerts"), "backend-prod", "1.9.1",
                directory.resolve("key"), directory, deliver = { false }).use { mirror ->
                val client = object : TelegramAlertClient {
                    override fun send(message: TelegramAlertMessage) = TelegramAlertResult(true)
                }
                val service = TelegramAlertService(client, async = false, agentMirror = mirror)
                assertTrue(service.error("first", "details", "fp").sent)
                assertEquals("dedup_suppressed", service.critical("second", "details", "fp").reason)
                assertEquals(2L, Files.list(directory).use { files -> files.count() })
            }
        } finally {
            Files.list(directory).use { it.forEach(Files::deleteIfExists) }
            Files.delete(directory)
        }
    }

    @Test
    fun `rejected events cannot starve later batch`() {
        val directory = Files.createTempDirectory("alert-fair")
        val seen = mutableSetOf<String>()
        try {
            DurableAgentAlertMirror(URI("https://alerts.example/alerts"), "backend-prod", "1.9.1",
                directory.resolve("key"), directory, deliver = { seen.add(it.toString(Charsets.UTF_8)); false }).use { mirror ->
                repeat(45) { mirror.enqueue(TelegramAlertMessage(AlertLevel.Error, "failure", "", "fp-$it")) }
                repeat(3) { mirror.flush() }
                assertEquals(45, seen.size)
                assertEquals(45L, Files.list(directory).use { files -> files.count() })
            }
        } finally {
            Files.list(directory).use { it.forEach(Files::deleteIfExists) }
            Files.delete(directory)
        }
    }

    @Test
    fun `unavailable receiver retains event across producer restart`() {
        val directory = Files.createTempDirectory("alert-spool")
        val event = TelegramAlertMessage(AlertLevel.Error, "failure", "details", "fp", Instant.EPOCH)
        try {
            DurableAgentAlertMirror(URI("https://alerts.example/alerts"), "backend-prod", "1.9.1",
                directory.resolve("key"), directory, deliver = { false }).use {
                assertTrue(it.enqueue(event))
                assertTrue(it.enqueue(event))
                it.flush()
                assertEquals(1L, Files.list(directory).use { files -> files.count() })
            }
            var sent = 0
            DurableAgentAlertMirror(URI("https://alerts.example/alerts"), "backend-prod", "1.9.1",
                directory.resolve("key"), directory, deliver = { sent++; true }).use {
                it.flush()
            }
            assertEquals(1, sent)
            assertEquals(0L, Files.list(directory).use { it.count() })
        } finally {
            Files.list(directory).use { it.forEach(Files::deleteIfExists) }
            Files.delete(directory)
        }
    }

    @Test
    fun `full spool does not claim successful delivery`() {
        val directory = Files.createTempDirectory("alert-full")
        try {
            DurableAgentAlertMirror(URI("https://alerts.example/alerts"), "backend-prod", "1.9.1",
                directory.resolve("key"), directory, maxPending = 1, deliver = { false }).use {
                assertTrue(it.enqueue(TelegramAlertMessage(AlertLevel.Warning, "first", "", "one")))
                assertFalse(it.enqueue(TelegramAlertMessage(AlertLevel.Critical, "second", "", "two")))
            }
        } finally {
            Files.list(directory).use { it.forEach(Files::deleteIfExists) }
            Files.delete(directory)
        }
    }
}
