package com.christian.nutriplan.plugins

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.christian.nutriplan.models.*
import com.christian.nutriplan.services.UsuarioService
import com.christian.nutriplan.models.responses.ApiResponse
import com.christian.nutriplan.models.responses.ApiResponse.LoginResponse
import org.slf4j.LoggerFactory
import java.util.Date


/**
 * Configuración de seguridad para la aplicación NutriPlan
 */
fun Application.configureSecurity(usuarioService: UsuarioService) {
    // Configuración secreta del JWT (preferible cargar desde configuración)
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()
    val logger = LoggerFactory.getLogger("SecurityConfig")
    // Configuración de autenticación JWT
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                val usuarioId = credential.payload.getClaim("userId").asInt()
                logger.info("Validando token para usuario ID: $usuarioId")
                try {
                    val principal = JWTPrincipal(credential.payload)
                    if (usuarioService.read(usuarioId) == null) {
                        logger.warn("Usuario $usuarioId no existe en BD pero token es válido")
                    }
                    principal
                } catch (e: Exception) {
                    logger.error("Error validando token: ${e.message}")
                    null
                }
            }
            challenge { _, _ ->
                logger.warn("Fallo de autenticación JWT")
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.Error(
                    message = "Acceso no autorizado",
                    error = "Token inválido o expirado"
                ))
            }
        }
    }

    // Rutas de autenticación
    routing {
        post("/login") {
            try {
                val credentials = call.receive<Credentials>()
                val usuario = usuarioService.login(credentials.email, credentials.contrasena)
                    ?: return@post call.respond(HttpStatusCode.Unauthorized,
                        ApiResponse.Error("Unauthorized", "Invalid credentials"))

                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("userId", usuario.usuarioId)
                    .withClaim("email", usuario.email)
                    .withClaim("rol", usuario.rol)
                    .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hour
                    .sign(Algorithm.HMAC256(jwtSecret))

                call.respond(HttpStatusCode.OK,
                    ApiResponse.Success(
                        data = LoginResponse(
                            token = token,
                            usuario = usuario.copy(contrasena = "")
                        ),
                        message = "Login successful"
                    )
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    ApiResponse.Error(
                        message = "Login failed",
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }




        post("/registro") {
            try {
                val nuevoUsuario = call.receive<Usuario>()
                val usuarioId = usuarioService.create(nuevoUsuario)
                val usuarioCreado = usuarioService.read(usuarioId)

                if (usuarioCreado != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse.Success(
                            data = usuarioCreado,
                            message = "Usuario creado exitosamente"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse.Error(
                            message = "No se pudo recuperar el usuario creado",
                            error = "Error interno del servidor"
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ApiResponse.Error(
                        message = "No se pudo crear el usuario",
                        error = e.message
                    )
                )
            }
        }
    }
}

// Función para generar un token JWT (uso auxiliar)
fun generateToken(userId: Int, jwtSecret: String, jwtIssuer: String, jwtAudience: String): String {
    return JWT.create()
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .withClaim("userId", userId)
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3600000)) // 1 hora
        .sign(Algorithm.HMAC256(jwtSecret))
}