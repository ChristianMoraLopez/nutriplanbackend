
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.christian"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)
    implementation(libs.postgresql)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.kotlin.asyncapi.ktor)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.http.redirect)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.default.headers)
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.ktor:ktor-server-forwarded-header:$3.1.2")
    // JWT Auth
    implementation("io.ktor:ktor-server-auth-jvm:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.7")
    implementation("com.auth0:java-jwt:4.4.0")

    // BCrypt para hashing de contraseñas
    implementation("com.ToxicBakery.library.bcrypt:bcrypt:+")




    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")
}
