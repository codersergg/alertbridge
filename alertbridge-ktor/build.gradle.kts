dependencies {
    api(project(":alertbridge-core"))
    compileOnly("io.ktor:ktor-server-core-jvm:2.3.12")
    testImplementation(kotlin("test"))
}
