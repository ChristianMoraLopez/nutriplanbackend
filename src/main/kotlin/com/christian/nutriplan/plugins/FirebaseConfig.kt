package com.christian.nutriplan.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.ByteArrayInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseCredentials(
    val type: String,
    val project_id: String,
    val private_key_id: String,
    val private_key: String,
    val client_email: String,
    val client_id: String,
    val auth_uri: String,
    val token_uri: String,
    val auth_provider_x509_cert_url: String,
    val client_x509_cert_url: String,
    val universe_domain: String
)

fun Application.configureFirebase() {
    val config = environment.config
    val credentials = FirebaseCredentials(
        type = config.property("firebase.credentials.type").getString(),
        project_id = config.property("firebase.credentials.project_id").getString(),
        private_key_id = config.property("firebase.credentials.private_key_id").getString(),
        private_key = config.property("firebase.credentials.private_key").getString(),
        client_email = config.property("firebase.credentials.client_email").getString(),
        client_id = config.property("firebase.credentials.client_id").getString(),
        auth_uri = config.property("firebase.credentials.auth_uri").getString(),
        token_uri = config.property("firebase.credentials.token_uri").getString(),
        auth_provider_x509_cert_url = config.property("firebase.credentials.auth_provider_x509_cert_url").getString(),
        client_x509_cert_url = config.property("firebase.credentials.client_x509_cert_url").getString(),
        universe_domain = config.property("firebase.credentials.universe_domain").getString()
    )

    // Convert credentials to JSON string
    val credentialsJson = Json.encodeToString(credentials)
    val credentialsStream = ByteArrayInputStream(credentialsJson.toByteArray(Charsets.UTF_8))

    val googleCredentials = GoogleCredentials.fromStream(credentialsStream)
    val options = FirebaseOptions.builder()
        .setCredentials(googleCredentials)
        .build()

    try {
        FirebaseApp.initializeApp(options)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to initialize Firebase: ${e.message}", e)
    }
}