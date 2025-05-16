package com.christian.nutriplan

import com.christian.nutriplan.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    try {
        val firebaseCredentialsJson = dotenv { ignoreIfMissing = true }["FIREBASE_CREDENTIALS"]
            ?: System.getenv("FIREBASE_CREDENTIALS")
            ?: throw IllegalStateException("FIREBASE_CREDENTIALS not found")
        logger.info("FIREBASE_CREDENTIALS length: ${firebaseCredentialsJson.length}")
        Json.parseToJsonElement(firebaseCredentialsJson) // Validate JSON
        val credentials = GoogleCredentials.fromStream(
            ByteArrayInputStream(firebaseCredentialsJson.toByteArray(Charsets.UTF_8))
        )
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId("nutriplan-d963a")
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            logger.info("Firebase initialized")
        }
    } catch (e: Exception) {
        logger.error("Firebase init failed: ${e.message}", e)
        throw IllegalStateException("Firebase initialization failed", e)
    }
    configureSerialization()
    val database = configureDatabase()
    val services = createServices(database)
    configureHTTP()
    configureSecurity(services.usuarioService)
    configureRouting(services)
}