
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.christian"
version = "0.0.1"

application {
    mainClass = "com.christian.nutriplan.ApplicationKt"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}
val ktorVersion = "2.3.7"

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json) // Asume que usa ktorVersion
    implementation(libs.ktor.server.content.negotiation) // Asume que usa ktorVersion
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2) // Para pruebas locales con H2
    implementation(libs.postgresql) // Para PostgreSQL
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.kotlin.asyncapi.ktor) // Verifica que la versión de esta librería sea compatible con ktorVersion
    implementation(libs.ktor.server.auth) // Asume que usa ktorVersion (o ya es 2.3.7)
    implementation(libs.ktor.server.openapi) // Verifica que la versión de esta librería sea compatible con ktorVersion
    implementation(libs.ktor.server.http.redirect) // Asume que usa ktorVersion
    implementation(libs.ktor.server.cors) // Asume que usa ktorVersion
    implementation(libs.ktor.server.netty) // Asume que usa ktorVersion
    implementation(libs.logback.classic) // Logging
    implementation(libs.ktor.server.config.yaml) // Asume que usa ktorVersion. Esta es la correcta para config YAML.
    implementation(libs.ktor.server.compression) // Asume que usa ktorVersion
    implementation(libs.ktor.server.default.headers) // Asume que usa ktorVersion

    // Corregido: Usar la variable ktorVersion y el artefacto JVM correcto.
    implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktorVersion")

    // Dependencias de autenticación JWT explícitas (ya estaban bien con 2.3.7)
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion") // Ya estaba como 2.3.7, mantenemos consistencia
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion") // Ya estaba como 2.3.7, mantenemos consistencia
    implementation("com.auth0:java-jwt:4.4.0") // JWT library (solo una vez)

    // BCrypt para hashing de contraseñas
    implementation("org.mindrot:jbcrypt:0.4") // jBCrypt
    implementation("com.ToxicBakery.library.bcrypt:bcrypt:+")
    // Dotenv
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Exposed Java Time
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.1")

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.4.3")

    // Eliminadas dependencias duplicadas o incorrectas:
    // - "com.auth0:java-jwt:4.4.0" (ya estaba arriba)
    // - "io.ktor:ktor-server-config-yaml:$3.1.2" (la versión era incorrecta y ya tienes libs.ktor.server.config.yaml)

    // Dependencias de Test
    testImplementation(libs.ktor.server.test.host) // Asume que usa ktorVersion y es la única necesaria.
    testImplementation(libs.kotlin.test.junit)
    // testImplementation(libs.ktor.server.test.host) // Eliminada la duplicada
}
