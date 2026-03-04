plugins {
    kotlin("jvm") version "1.9.25" apply false
    `maven-publish`
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/codersergg/alertbridge")
                credentials {
                    username = providers.gradleProperty("githubPackagesUser")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orNull
                    password = providers.gradleProperty("githubPackagesToken")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orNull
                }
            }
        }
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    name.set(project.name)
                    description.set("AlertBridge - Telegram alerting for JVM apps")
                    url.set("https://github.com/codersergg/alertbridge")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("codersergg")
                            name.set("Sergey")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/codersergg/alertbridge.git")
                        developerConnection.set("scm:git:ssh://github.com/codersergg/alertbridge.git")
                        url.set("https://github.com/codersergg/alertbridge")
                    }
                }
            }
        }
    }
}
