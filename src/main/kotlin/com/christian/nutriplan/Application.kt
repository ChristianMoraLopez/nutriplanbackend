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
    logger.info("Starting module configuration")

    try {
        // Load from dotenv or System.getenv
        val dotenv = dotenv {
            ignoreIfMissing = true
        }
        val firebaseCredentialsJson = dotenv["FIREBASE_CREDENTIALS"] ?: System.getenv("FIREBASE_CREDENTIALS")
        ?: throw IllegalStateException("FIREBASE_CREDENTIALS environment variable not found")
        logger.info("FIREBASE_CREDENTIALS found, length: ${firebaseCredentialsJson.length}")

        // Validate JSON format
        try {
            Json.parseToJsonElement(firebaseCredentialsJson)
            logger.info("Firebase credentials JSON is valid")
        } catch (e: Exception) {
            logger.error("Invalid Firebase credentials JSON: ${e.message}")
            throw IllegalStateException("Invalid Firebase credentials JSON", e)
        }

        val credentials = GoogleCredentials.fromStream(
            ByteArrayInputStream(firebaseCredentialsJson.toByteArray())
        )
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId("nutriplan-d963a")
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            logger.info("Firebase Admin SDK initialized successfully")
        } else {
            logger.info("Firebase Admin SDK already initialized")
        }
    } catch (e: Exception) {
        logger.error("Failed to initialize Firebase Admin SDK: ${e.javaClass.simpleName}: ${e.message}", e)
        throw IllegalStateException("Firebase initialization failed", e)
    }

    // Configure serialization
    logger.info("Configuring serialization")
    configureSerialization()

    // Configure database
    logger.info("Configuring database")
    val database = configureDatabase()

    // Create services
    logger.info("Creating services")
    val services = createServices(database)

    // Configure HTTP, security, and routing
    logger.info("Configuring HTTP")
    configureHTTP()
    logger.info("Configuring security")
    configureSecurity(services.usuarioService)
    logger.info("Configuring routing")
    configureRouting(services)
    logger.info("Module configuration completed")
}