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
import com.google.firebase.FirebaseApp
import org.slf4j.LoggerFactory
import com.google.firebase.auth.FirebaseAuth
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.Date
fun Application.configureSecurity(usuarioService: UsuarioService) {

    val logger = LoggerFactory.getLogger("SecurityConfig")

    // Load JWT configuration with fallbacks
    val jwtSecret = try {
        environment.config.property("jwt.secret").getString()
    } catch (e: Exception) {
        logger.error("Failed to load jwt.secret from config: ${e.message}")
        System.getenv("JWT_SECRET") ?: run {
            logger.error("JWT_SECRET not found in environment variables")
            throw IllegalStateException("JWT secret configuration is required")
        }
    }
    val jwtIssuer = try {
        environment.config.property("jwt.issuer").getString()
    } catch (e: Exception) {
        logger.error("Failed to load jwt.issuer from config: ${e.message}")
        "nutriplan-api" // Fallback value
    }
    val jwtAudience = try {
        environment.config.property("jwt.audience").getString()
    } catch (e: Exception) {
        logger.error("Failed to load jwt.audience from config: ${e.message}")
        "nutriplan-users" // Fallback value
    }
    val jwtRealm = try {
        environment.config.property("jwt.realm").getString()
    } catch (e: Exception) {
        logger.error("Failed to load jwt.realm from config: ${e.message}")
        "NutriPlan App" // Fallback value
    }

    logger.info("JWT Configuration - secret: [REDACTED], issuer: $jwtIssuer, audience: $jwtAudience, realm: $jwtRealm")
    // Rest of the code remains unchanged
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
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.Error(
                    message = "Acceso no autorizado",
                    error = "Token inválido o expirado"
                ))
            }
        }

        jwt("auth-firebase") {
            realm = jwtRealm
            validate { credential ->
                try {
                    val idToken = this.request.header("Authorization")?.removePrefix("Bearer ") ?: return@validate null
                    val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)
                    val email = decodedToken.email
                    val usuario = usuarioService.findByEmail(email)

                    if (usuario != null) {
                        val internalJwtString = JWT.create()
                            .withClaim("userId", usuario.usuarioId)
                            .withClaim("email", email)
                            .withClaim("rol", usuario.rol)
                            .withAudience(jwtAudience)
                            .withIssuer(jwtIssuer)
                            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
                            .sign(Algorithm.HMAC256(jwtSecret))
                        JWTPrincipal(JWT.decode(internalJwtString))
                    } else {
                        logger.warn("User not found in DB for Firebase email: $email")
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Error validating Firebase token: ${e.message}")
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.Error(
                    message = "Invalid or expired Firebase token",
                    error = "Firebase authentication failed"
                ))
            }
        }
    }

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
                    .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
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

        post("/google-login") {
            val logger = LoggerFactory.getLogger("GoogleLoginEndpoint")
            try {
                val request = call.receive<GoogleLoginRequest>()
                logger.info("Received Google login request with token: ${request.idToken.take(10)}...")

                // Verify Firebase initialization
                if (FirebaseApp.getApps().isEmpty()) {
                    logger.error("Firebase Admin SDK not initialized")
                    throw IllegalStateException("Firebase Admin SDK not initialized")
                }

                logger.info("Verifying Firebase token...")
                val decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.idToken)
                logger.info("Firebase token verified, email: ${decodedToken.email}, uid: ${decodedToken.uid}")

                val email = decodedToken.email ?: throw IllegalArgumentException("Email not provided in token")
                val nombre = decodedToken.name ?: "User ${decodedToken.uid}"

                var usuario = usuarioService.findByEmail(email)
                logger.info("findByEmail result for $email: ${usuario != null}")
                if (usuario == null) {
                    logger.info("Creating new user for email: $email")
                    usuario = Usuario(
                        usuarioId = 0,
                        nombre = nombre,
                        email = email,
                        contrasena = "",
                        aceptaTerminos = true,
                        rol = "usuario",
                        fechaRegistro = LocalDateTime.now().toString(),
                        ciudad = "",
                        localidad = ""
                    )
                    val newUserId = usuarioService.create(usuario)
                    logger.info("New user created with ID: $newUserId")
                    usuario = usuarioService.read(newUserId) ?: run {
                        logger.error("Failed to read created user with ID: $newUserId")
                        throw IllegalStateException("Failed to read created user")
                    }
                } else {
                    logger.info("Existing user found with ID: ${usuario.usuarioId}")
                }

                val jwtToken = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("userId", usuario.usuarioId)
                    .withClaim("email", usuario.email)
                    .withClaim("rol", usuario.rol)
                    .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
                    .sign(Algorithm.HMAC256(jwtSecret))

                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.Success(
                        data = LoginResponse(
                            token = jwtToken,
                            usuario = usuario.copy(contrasena = "")
                        ),
                        message = "Google login successful"
                    )
                )
            } catch (e: Exception) {
                logger.error("Google login error: ${e.javaClass.simpleName}: ${e.message}", e)
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.Error(
                        message = "Invalid Google token",
                        error = "${e.javaClass.simpleName}: ${e.message}"
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
@Serializable
data class GoogleLoginRequest(val idToken: String)

fun generateToken(userId: Int, jwtSecret: String, jwtIssuer: String, jwtAudience: String): String {
    return JWT.create()
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
        .sign(Algorithm.HMAC256(jwtSecret))
}