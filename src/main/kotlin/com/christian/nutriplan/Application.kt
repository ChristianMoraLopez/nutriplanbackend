package com.christian.nutriplan

import com.christian.nutriplan.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.FileInputStream

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting module configuration")


    try {
        val serviceAccountPath = "src/main/resources/nutriplan-d963a-firebase-adminsdk-fbsvc-c62cad2f50.json"
        logger.info("Loading Firebase credentials from: $serviceAccountPath")
        val credentials = GoogleCredentials.fromStream(FileInputStream(serviceAccountPath))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId("nutriplan-d963a") // <--- ¡AÑADE ESTA LÍNEA EXPLÍCITAMENTE!
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
            logger.info("Firebase Admin SDK initialized successfully FROM FILE.")
        } else {
            logger.info("Firebase Admin SDK already initialized.")
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