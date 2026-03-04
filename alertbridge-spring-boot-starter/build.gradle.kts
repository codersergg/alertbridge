plugins {
    kotlin("plugin.spring") version "1.9.25"
}

dependencies {
    api(project(":alertbridge-core"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.5.4")
    compileOnly("org.springframework.boot:spring-boot-starter:3.5.4")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:3.5.4")
    compileOnly("org.springframework:spring-web:6.2.9")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.4")
}
