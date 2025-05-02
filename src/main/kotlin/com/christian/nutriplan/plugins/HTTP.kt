package com.christian.nutriplan.plugins

import io.ktor.http.*
import io.ktor.http.content.CachingOptions
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.* // Import for Compression
import io.ktor.server.plugins.defaultheaders.* // Import for DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.* // Import for ForwardedHeaders

/**
 * Configuración de plugins HTTP para la aplicación NutriPlan
 */
fun Application.configureHTTP() {
    // Configuración de CORS para permitir peticiones desde cualquier origen
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-CSRF-Token")
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowSameOrigin = true
        // Permitir peticiones desde cualquier origen (cuidado en producción)
        anyHost()
    }

    // Configuración de compresión de respuestas
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // No comprimir respuestas muy pequeñas
        }
    }

    // Cabeceras por defecto para todas las respuestas
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // Una cabecera adicional
        header("X-Application", "NutriPlan")
    }

    // Manejo de cabeceras de redirección (X-Forwarded-*)
    install(ForwardedHeaders)

    // Configuración de cabeceras de caché
    // Configuración de cabeceras de caché
    install(CachingHeaders) {
        options { call, outgoingContent ->
            // Configuración de caché para recursos estáticos
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS, ContentType.Text.JavaScript -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))
                ContentType.Image.PNG, ContentType.Image.JPEG -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 86400))
                else -> null
            }
        }
    }
}