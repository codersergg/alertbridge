package com.codersergg.alertbridge.core

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Persist before acknowledging; an unavailable agent never consumes an event. */
class DurableAgentAlertMirror(
    private val endpoint: URI,
    private val source: String,
    private val version: String,
    private val keyFile: Path,
    private val spool: Path,
    private val maxPending: Int = 1000,
    private val deliver: ((ByteArray) -> Boolean)? = null,
) : AutoCloseable {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "alertbridge-agent-mirror").apply { isDaemon = true }
    }
    private val monitor = Any()
    private var started = false
    private var cursor: String? = null

    init {
        require(endpoint.scheme == "https" && endpoint.host != null && endpoint.rawUserInfo == null &&
            endpoint.path in setOf("/alerts", "/_agent/alerts") && endpoint.rawQuery == null && endpoint.rawFragment == null)
        require(source.matches(Regex("[a-z0-9-]{1,64}")) && version.isNotBlank() && version.length <= 128)
        require(spool.isAbsolute && keyFile.isAbsolute && maxPending in 1..1000)
        val privatePermissions = java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")
        val posix = spool.fileSystem.supportedFileAttributeViews().contains("posix")
        if (posix) {
            Files.createDirectories(spool, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(privatePermissions))
        } else {
            Files.createDirectories(spool)
        }
        require(!Files.isSymbolicLink(spool) && Files.isDirectory(spool))
        // Never chmod an existing, possibly shared directory selected by mistake.
        if (posix) require(Files.getPosixFilePermissions(spool) == privatePermissions)
    }

    fun enqueue(message: TelegramAlertMessage): Boolean = synchronized(monitor) {
        require(message.fingerprint.isNotBlank() && message.fingerprint.toByteArray().size <= 512)
        require(message.title.isNotBlank() && message.title.toByteArray().size <= 512)
        require(message.details.toByteArray().size <= 16384)
        val fields = linkedMapOf(
            "level" to message.level.label(), "fingerprint" to message.fingerprint,
            "version" to version, "at" to message.at.toString(),
            "title" to message.title, "details" to message.details,
        )
        require(fields.values.none { it.contains('\u0000') })
        val body = fields.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
        }.toByteArray(StandardCharsets.UTF_8)
        val name = MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { "%02x".format(it) }
        val target = spool.resolve("$name.alert")
        if (Files.exists(target)) return@synchronized true
        val pending = Files.list(spool).use { paths -> paths.filter { it.fileName.toString().endsWith(".alert") }.count() }
        if (pending >= maxPending) return@synchronized false
        val temp = Files.createTempFile(spool, ".pending-", ".tmp")
        try {
            FileChannel.open(temp, StandardOpenOption.WRITE).use { channel ->
                val data = java.nio.ByteBuffer.wrap(body)
                while (data.hasRemaining()) channel.write(data)
                channel.force(true)
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
            forceDirectory()
        } finally {
            Files.deleteIfExists(temp)
        }
        true
    }

    fun start() = synchronized(monitor) {
        if (!started) {
            started = true
            scheduler.scheduleWithFixedDelay({ runCatching { flush() } }, 0, 10, TimeUnit.SECONDS)
        }
    }

    fun flush() {
        val files = synchronized(monitor) {
            val pending = Files.list(spool).use { paths -> paths.filter { it.fileName.toString().matches(Regex("[a-f0-9]{64}\\.alert")) }
                .sorted().toList() }
            // Round-robin keeps a permanently rejected event from starving later ones.
            val after = pending.filter { cursor == null || it.fileName.toString() > cursor!! }
            (after + pending.filter { it !in after }).take(20).also {
                if (it.isNotEmpty()) cursor = it.last().fileName.toString()
            }
        }
        for (file in files) {
            val accepted = runCatching {
                require(!Files.isSymbolicLink(file) && Files.size(file) <= 96 * 1024)
                val body = Files.readAllBytes(file)
                deliver?.invoke(body) ?: send(body)
            }.getOrDefault(false)
            if (!accepted) continue
            synchronized(monitor) {
                Files.delete(file)
                forceDirectory()
            }
        }
    }

    private fun send(body: ByteArray): Boolean {
        val key = Files.readAllBytes(keyFile).toString(StandardCharsets.UTF_8).trim().toByteArray()
        require(key.size >= 32)
        val at = Instant.now().epochSecond.toString()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val signature = mac.doFinal(source.toByteArray() + byteArrayOf(10) + at.toByteArray() + byteArrayOf(10) + body)
            .joinToString("") { "%02x".format(it) }
        val request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("X-Alert-Source", source).header("X-Alert-Time", at)
            .header("X-Alert-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build()
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 202
    }

    private fun forceDirectory() {
        if (spool.fileSystem.supportedFileAttributeViews().contains("posix")) {
            FileChannel.open(spool, StandardOpenOption.READ).use { it.force(true) }
        }
    }

    override fun close() {
        scheduler.shutdown()
        scheduler.awaitTermination(6, TimeUnit.SECONDS)
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): DurableAgentAlertMirror? {
            val endpoint = env["ALERTBRIDGE_AGENT_URL"] ?: return null
            return DurableAgentAlertMirror(
                URI.create(endpoint), requireNotNull(env["ALERTBRIDGE_AGENT_SOURCE"]),
                requireNotNull(env["APP_VERSION"] ?: env["APP_ALERTS_TELEGRAM_SERVICE_VERSION"]), Path.of(requireNotNull(env["ALERTBRIDGE_AGENT_KEY_FILE"])),
                Path.of(requireNotNull(env["ALERTBRIDGE_AGENT_SPOOL"])),
            ).also { it.start() }
        }
    }
}
