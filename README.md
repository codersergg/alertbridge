# AlertBridge

Lightweight Telegram alerting library for JVM applications.

- Works in any JVM code (`alertbridge-core`)
- Spring Boot starter (`alertbridge-spring-boot-starter`)
- Ktor integration (`alertbridge-ktor`)
- No runtime dependency on LingFlow code

## Modules

- `alertbridge-core`: generic JVM API + default Telegram HTTP client.
- `alertbridge-spring-boot-starter`: auto-configured beans for Spring Boot.
- `alertbridge-ktor`: Ktor plugin + helper accessor.

## Compatibility Matrix

| Area | Supported versions | CI coverage |
|---|---|---|
| Java | 17, 21 | 17 + 21 |
| Spring Boot starter | 3.2+ | 3.2.12, 3.3.13, 3.4.7, 3.5.4 |
| Ktor integration | 2.3.x baseline | Built on every CI run |

## Coordinates

Group: `com.codersergg`

Artifacts:

- `com.codersergg:alertbridge-core:<version>`
- `com.codersergg:alertbridge-spring-boot-starter:<version>`
- `com.codersergg:alertbridge-ktor:<version>`

## Repository Access

### Public without credentials

Use Maven Central. Consumers need only:

```kotlin
repositories {
    mavenCentral()
}
```

### Current publish target in this repository

GitHub Packages is configured right now:

- https://github.com/codersergg/alertbridge/packages

GitHub Packages requires credentials even for public packages. Use it only as an additional/private channel:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/codersergg/alertbridge")
        credentials {
            username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

---

## 1) Plain JVM Usage

### Gradle

```kotlin
dependencies {
    implementation("com.codersergg:alertbridge-core:<version>")
}
```

### Example

```kotlin
import com.codersergg.alertbridge.core.*
import java.time.Duration

val config = TelegramAlertConfig(
    enabled = true,
    botToken = System.getenv("ALERT_TELEGRAM_BOT_TOKEN"),
    chatId = System.getenv("ALERT_TELEGRAM_CHAT_ID"),
    messageThreadId = System.getenv("ALERT_TELEGRAM_THREAD_ID")?.toLongOrNull(),
    connectTimeout = Duration.ofSeconds(2),
    requestTimeout = Duration.ofSeconds(3),
    dedupWindow = Duration.ofMinutes(2),
)

val client = DefaultTelegramAlertClient(config)
val alerts = TelegramAlertService(
    client = client,
    dedupWindowSeconds = 120,
    async = true,
    queueCapacity = 1024,
    overflowPolicy = AlertQueueOverflowPolicy.DropOldest,
)
val serviceAlerts = alerts.scoped(serviceName = "billing-service", version = "1.2.3")

serviceAlerts.critical(
    event = "db_degraded",
    fingerprint = "db-p99-degraded",
    fields = mapOf("reason" to "p99_latency_high"),
)
```

### API Summary

- `TelegramAlertService.info(...)`
- `TelegramAlertService.warning(...)`
- `TelegramAlertService.error(...)`
- `TelegramAlertService.critical(...)`
- `TelegramAlertService.send(...)`
- `TelegramAlertService.scoped(serviceName, version)` -> one-line service-scoped API:
- `ScopedTelegramAlertService.info/warning/error/critical(details, fingerprint)`
- `ScopedTelegramAlertService.info/warning/error/critical(event, fingerprint, fields)`
- `ScopedTelegramAlertService.infoRaw/warningRaw/errorRaw/criticalRaw(details, fingerprint)`
- `TelegramAlertNotifier.notifyInfo/notifyWarning/notifyCritical(details, fingerprint)`

Each method returns `TelegramAlertResult(sent: Boolean, reason: String?)`.

`fingerprint` is used for built-in de-dup suppression within the dedup window.

---

## 2) Spring Boot Starter

### Gradle

```kotlin
dependencies {
    implementation("com.codersergg:alertbridge-spring-boot-starter:<version>")
}
```

### Configuration

```yaml
alertbridge:
  telegram:
    enabled: true
    bot-token: ${ALERT_TELEGRAM_BOT_TOKEN}
    chat-id: ${ALERT_TELEGRAM_CHAT_ID}
    message-thread-id: ${ALERT_TELEGRAM_THREAD_ID:}
    connect-timeout-ms: 2000
    request-timeout-ms: 3000
    dedup-window-seconds: 120
    service-name: my-service
    service-version: 1.2.3
    async: true
    queue-capacity: 1024
    overflow-policy: DropOldest
```

### Inject and Use (Config-Only in App Code)

```kotlin
import com.codersergg.alertbridge.core.TelegramAlertNotifier
import org.springframework.stereotype.Service

@Service
class IncidentReporter(
    private val alerts: TelegramAlertNotifier,
) {
    fun onCriticalFailure(error: Throwable) {
        alerts.notifyCritical(
            details = "event=critical_failure reason=${error.message ?: "unknown"}",
            fingerprint = "critical-failure",
        )
    }
}
```

### Auto-configured Beans

- `TelegramAlertConfig`
- `TelegramAlertClient`
- `TelegramAlertService`
- `ScopedTelegramAlertService`
- `TelegramAlertNotifier` (real when enabled, `NoopTelegramAlertNotifier` when disabled)

`TelegramAlertNotifier` bean is always available for injection.
Underlying Telegram sender beans are created only when `alertbridge.telegram.enabled=true`.

---

## 3) Ktor Integration

### Gradle

```kotlin
dependencies {
    implementation("com.codersergg:alertbridge-ktor:<version>")
}
```

### Install Plugin

```kotlin
import com.codersergg.alertbridge.core.TelegramAlertConfig
import com.codersergg.alertbridge.ktor.AlertBridgeKtorPlugin
import io.ktor.server.application.install

fun Application.module() {
    install(AlertBridgeKtorPlugin) {
        config = TelegramAlertConfig(
            enabled = true,
            botToken = System.getenv("ALERT_TELEGRAM_BOT_TOKEN"),
            chatId = System.getenv("ALERT_TELEGRAM_CHAT_ID"),
        )
        dedupWindowSeconds = 120
    }
}
```

### Access in Ktor Code

```kotlin
import com.codersergg.alertbridge.ktor.alertBridge

val alerts = application.alertBridge()
alerts.error(
    title = "External API failure",
    details = "provider timeout",
    fingerprint = "provider-timeout",
    service = "ktor-gateway",
)
```

---

## Operational Notes

- Keep Telegram bot token in secret storage only.
- Keep chat/thread identifiers in config.
- Use stable fingerprints for dedup (e.g. `component-error-code`).
- Send concise details; avoid secrets/PII in alert text.

## Roadmap

- Async/non-blocking sender mode
- Retry and backoff strategies
- Structured templating
- Micrometer integration module

## GitHub Actions

Included workflows:

- `CI` (`.github/workflows/ci.yml`): build + tests on push/PR with matrix:
  - Java: 17, 21
  - Spring Boot (starter compile/test baseline): 3.2.12, 3.3.13, 3.4.7, 3.5.4
- `Publish` (`.github/workflows/publish.yml`): publish artifacts to GitHub Packages on `v*` tags and manual run.

### Release flow

1. Update version in `gradle.properties`.
2. Commit and push to `main`.
3. Create tag, e.g. `v0.1.0`, and push tag.
4. GitHub Actions `Publish` workflow uploads artifacts.

## License

MIT
