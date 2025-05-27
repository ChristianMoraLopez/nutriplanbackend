package com.christian.nutriplan.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.christian.nutriplan.models.*
import com.christian.nutriplan.models.responses.ApiResponse
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun Application.configureRouting(services: Services) {
    @Serializable
    data class ErrorResponse(
        val message: String,
        val error: String? = null
    )

    suspend fun <T> validateId(
        call: ApplicationCall,
        parameter: String,
        block: suspend (Int) -> T
    ) {
        val id = call.parameters[parameter]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("ID inválido"))
            return
        }
        block(id)
    }

    fun Route.handleCrud(
        routePath: String,
        entityName: String,
        getAll: suspend () -> List<Any>,
        read: suspend (Int) -> Any?,
        create: suspend (Any) -> Int,
        update: suspend (Int, Any) -> Unit,
        delete: suspend (Int) -> Unit,
        receiveType: KClass<*>
    ) {
        route(routePath) {
            get {
                call.respond(getAll())
            }

            get("/{id}") {
                validateId(call, "id") { id ->
                    val entity = read(id)
                    if (entity != null) {
                        call.respond(entity)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("$entityName no encontrado"))
                    }
                }
            }

            post {
                val entity = call.receive(receiveType)
                try {
                    val id = create(entity)
                    val createdEntity = read(id) ?: entity
                    call.respond(HttpStatusCode.Created, createdEntity)
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("No se pudo crear el $entityName: ${e.message}")
                    )
                }
            }

            put("/{id}") {
                validateId(call, "id") { id ->
                    val updatedEntity = call.receive(receiveType)
                    try {
                        update(id, updatedEntity)
                        call.respond(HttpStatusCode.OK, mapOf("message" to "$entityName actualizado correctamente"))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("No se pudo actualizar el $entityName: ${e.message}")
                        )
                    }
                }
            }

            delete("/{id}") {
                validateId(call, "id") { id ->
                    try {
                        delete(id)
                        call.respond(HttpStatusCode.OK, mapOf("message" to "$entityName eliminado correctamente"))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("No se pudo eliminar el $entityName: ${e.message}")
                        )
                    }
                }
            }
        }
    }

    fun ApplicationCall.getUserId(): Int? {
        val principal = principal<JWTPrincipal>()
        return principal?.payload?.getClaim("userId")?.asInt()
    }

    routing {
        get("/") {
            call.respondText("API de NutriPlan - Sistema de Planificación Nutricional")
        }

        route("/usuarios") {
            get {
                val usuarios = services.usuarioService.getAllUsers()
                call.respond(usuarios)
            }

            get("/{id}") {
                validateId(call, "id") { id ->
                    val usuario = services.usuarioService.read(id)
                    if (usuario != null) {
                        val response = UsuarioResponse(
                            usuarioId = usuario.usuarioId
                                ?: throw IllegalStateException("ID de usuario no puede ser nulo"),
                            nombre = usuario.nombre,
                            email = usuario.email,
                            rol = usuario.rol,
                            fechaRegistro = usuario.fechaRegistro,
                            aceptaTerminos = usuario.aceptaTerminos
                        )
                        call.respond(response)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Usuario no encontrado"))
                    }
                }
            }

            get("/paginated") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
                val usuarios = services.usuarioService.getUsersPaginated(page, size)
                call.respond(usuarios)
            }
        }

        authenticate("auth-jwt", "auth-firebase") {
            val logger = LoggerFactory.getLogger("UserEndpoint")

            post("/usuarios") {
                val usuario = try {
                    call.receive<Usuario>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Datos de usuario inválidos: ${e.message}")
                    )
                    return@post
                }

                try {
                    val id = services.usuarioService.create(usuario)
                    val createdUsuario = services.usuarioService.read(id)?.copy(contrasena = "")
                        ?: throw IllegalStateException("No se pudo leer el usuario creado")
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse.Success(
                            data = createdUsuario,
                            message = "Usuario creado correctamente"
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorResponse("No se pudo crear el usuario: ${e.message}")
                    )
                }
            }

            get("/usuarios/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt()

                if (userId == null) {
                    return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse.Error(
                            message = "Token inválido",
                            error = "No se pudo obtener el ID de usuario del token"
                        )
                    )
                }

                val usuario = services.usuarioService.read(userId)?.copy(contrasena = "")
                if (usuario != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse.Success(
                            data = usuario,
                            message = "Usuario obtenido correctamente"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse.Error(
                            message = "Usuario no encontrado",
                            error = "El ID de usuario no existe"
                        )
                    )
                }
            }

            put("/usuarios/{id}") {
                val userId = call.getUserId()
                if (userId == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse.Error(
                            message = "Usuario no autenticado",
                            error = "Token inválido"
                        )
                    )
                    return@put
                }

                validateId(call, "id") { id ->
                    if (id != userId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiResponse.Error(
                                message = "No tienes permiso para modificar este usuario",
                                error = "ID mismatch"
                            )
                        )
                        return@validateId
                    }

                    val updatedUsuario = try {
                        call.receive<Usuario>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse.Error(
                                message = "Datos de usuario inválidos",
                                error = e.message
                            )
                        )
                        return@validateId
                    }

                    try {
                        val existingUsuario = services.usuarioService.read(id)
                        if (existingUsuario == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse.Error(
                                    message = "Usuario no encontrado",
                                    error = "El ID de usuario no existe"
                                )
                            )
                            return@validateId
                        }

                        services.usuarioService.update(id, updatedUsuario.copy(
                            usuarioId = id,
                            aceptaTerminos = existingUsuario.aceptaTerminos,
                            rol = existingUsuario.rol,
                            fechaRegistro = existingUsuario.fechaRegistro
                        ))
                        val updated = services.usuarioService.read(id)?.copy(contrasena = "")
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse.Success(
                                data = updated,
                                message = "Usuario actualizado correctamente"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse.Error(
                                message = "No se pudo actualizar el usuario",
                                error = e.message
                            )
                        )
                    }
                }
            }

            handleCrud(
                routePath = "/categorias",
                entityName = "Categoría",
                getAll = { services.categoriaIngredienteService.getAll() },
                read = { id -> services.categoriaIngredienteService.read(id) },
                create = { entity -> services.categoriaIngredienteService.create(entity as CategoriaIngrediente) },
                update = { id, entity ->
                    services.categoriaIngredienteService.update(id, entity as CategoriaIngrediente)
                },
                delete = { id -> services.categoriaIngredienteService.delete(id) },
                receiveType = CategoriaIngrediente::class
            )

            route("/ingredientes") {
                get {
                    call.respond(services.ingredienteService.getAllIngredientes())
                }

                get("/search") {
                    val query = call.parameters["q"] ?: ""
                    call.respond(services.ingredienteService.searchByName(query))
                }

                get("/categoria/{categoriaId}") {
                    validateId(call, "categoriaId") { categoriaId ->
                        call.respond(services.ingredienteService.getByCategoria(categoriaId))
                    }
                }

                handleCrud(
                    routePath = "",
                    entityName = "Ingrediente",
                    getAll = { services.ingredienteService.getAllIngredientes() },
                    read = { id -> services.ingredienteService.read(id) },
                    create = { entity -> services.ingredienteService.create(entity as Ingrediente) },
                    update = { id, entity -> services.ingredienteService.update(id, entity as Ingrediente) },
                    delete = { id -> services.ingredienteService.delete(id) },
                    receiveType = Ingrediente::class
                )
            }

            handleCrud(
                routePath = "/metodos",
                entityName = "Método",
                getAll = { services.metodoPreparacionService.getAll() },
                read = { id -> services.metodoPreparacionService.read(id) },
                create = { entity -> services.metodoPreparacionService.create(entity as MetodoPreparacion) },
                update = { id, entity ->
                    services.metodoPreparacionService.update(id, entity as MetodoPreparacion)
                },
                delete = { id -> services.metodoPreparacionService.delete(id) },
                receiveType = MetodoPreparacion::class
            )

            handleCrud(
                routePath = "/tipos_comida",
                entityName = "Tipo de Comida",
                getAll = { services.tipoComidaService.getAll() },
                read = { id -> services.tipoComidaService.read(id) },
                create = { entity -> services.tipoComidaService.create(entity as TipoComida) },
                update = { id, entity -> services.tipoComidaService.update(id, entity as TipoComida) },
                delete = { id -> services.tipoComidaService.delete(id) },
                receiveType = TipoComida::class
            )

            handleCrud(
                routePath = "/recetas",
                entityName = "Receta",
                getAll = { services.recetaService.getAll() },
                read = { id -> services.recetaService.read(id) },
                create = { entity -> services.recetaService.create(entity as Receta) },
                update = { id, entity -> services.recetaService.update(id, entity as Receta) },
                delete = { id -> services.recetaService.delete(id) },
                receiveType = Receta::class
            )

            route("/receta_ingredientes") {
                get {
                    call.respond(services.recetaIngredienteService.getAll())
                }

                get("/receta/{recetaId}") {
                    validateId(call, "recetaId") { recetaId ->
                        call.respond(services.recetaIngredienteService.getByReceta(recetaId))
                    }
                }

                post {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@post
                    }

                    val recetaIngrediente = try {
                        call.receive<RecetaIngrediente>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Datos de receta_ingrediente inválidos: ${e.message}")
                        )
                        return@post
                    }

                    try {
                        services.recetaIngredienteService.create(recetaIngrediente)
                        call.respond(HttpStatusCode.Created, recetaIngrediente)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse("No se pudo crear la relación receta-ingrediente: ${e.message}")
                        )
                    }
                }

                delete("/{recetaId}/{ingredienteId}") {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@delete
                    }

                    validateId(call, "recetaId") { recetaId ->
                        validateId(call, "ingredienteId") { ingredienteId ->
                            try {
                                services.recetaIngredienteService.delete(recetaId, ingredienteId)
                                call.respond(
                                    HttpStatusCode.OK,
                                    mapOf("message" to "Relación receta-ingrediente eliminada correctamente")
                                )
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse("No se pudo eliminar la relación receta-ingrediente: ${e.message}")
                                )
                            }
                        }
                    }
                }
            }

            route("/recetas_guardadas") {
                get {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@get
                    }

                    try {
                        val recetasGuardadas = services.recetaGuardadaService.getByUsuario(userId)
                        call.respond(recetasGuardadas)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("Error al obtener recetas guardadas: ${e.message}")
                        )
                    }
                }

                post {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@post
                    }

                    val recetaGuardada = try {
                        call.receive<RecetaGuardada>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Datos de receta guardada inválidos: ${e.message}")
                        )
                        return@post
                    }

                    if (recetaGuardada.usuarioId != userId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("No puedes guardar recetas para otro usuario")
                        )
                        return@post
                    }

                    try {
                        val id = services.recetaGuardadaService.create(recetaGuardada)
                        val createdRecetaGuardada = services.recetaGuardadaService.read(id)
                        if (createdRecetaGuardada != null) {
                            call.respond(HttpStatusCode.Created, createdRecetaGuardada)
                        } else {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo leer la receta guardada creada")
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse("No se pudo guardar la receta: ${e.message}")
                        )
                    }
                }

                put("/{id}") {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@put
                    }

                    validateId(call, "id") { id ->
                        try {
                            val existingRecetaGuardada = services.recetaGuardadaService.read(id)
                            if (existingRecetaGuardada == null || existingRecetaGuardada.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No tienes permiso para modificar esta receta guardada")
                                )
                                return@validateId
                            }

                            val updatedRecetaGuardada = call.receive<RecetaGuardada>()
                            if (updatedRecetaGuardada.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No puedes modificar el usuario de la receta guardada")
                                )
                                return@validateId
                            }

                            services.recetaGuardadaService.update(id, updatedRecetaGuardada)
                            val updated = services.recetaGuardadaService.read(id)
                            if (updated != null) {
                                call.respond(HttpStatusCode.OK, updated)
                            } else {
                                call.respond(
                                    HttpStatusCode.InternalServerError,
                                    ErrorResponse("No se pudo leer la receta guardada actualizada")
                                )
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo actualizar la receta guardada: ${e.message}")
                            )
                        }
                    }
                }

                delete("/{id}") {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@delete
                    }

                    validateId(call, "id") { id ->
                        try {
                            val existingRecetaGuardada = services.recetaGuardadaService.read(id)
                            if (existingRecetaGuardada == null || existingRecetaGuardada.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No tienes permiso para eliminar esta receta guardada")
                                )
                                return@validateId
                            }

                            services.recetaGuardadaService.delete(id)
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf("message" to "Receta guardada eliminada correctamente")
                            )
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo eliminar la receta guardada: ${e.message}")
                            )
                        }
                    }
                }
            }
        }

        route("/public") {
            get("/categorias") {
                call.respond(services.categoriaIngredienteService.getAll())
            }

            get("/ingredientes") {
                call.respond(services.ingredienteService.getAllIngredientes())
            }

            get("/tipos_comida") {
                call.respond(services.tipoComidaService.getAll())
            }

            get("/metodos") {
                call.respond(services.metodoPreparacionService.getAll())
            }

            get("/recetas") {
                call.respond(services.recetaService.getAll())
            }
        }
    }
}