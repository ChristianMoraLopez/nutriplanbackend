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
                            aceptaTerminos = usuario.aceptaTerminos,
                            ciudad = usuario.ciudad,
                            localidad = usuario.localidad
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



        authenticate("auth-jwt") {
            // Agrega esto al inicio de tu archivo (fuera de las funciones)
            val logger = LoggerFactory.getLogger("UserEndpoint")
            // Agrega este endpoint a tu routing


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
                    call.respond(services.ingredienteService.getAll())
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
                    getAll = { services.ingredienteService.getAll() },
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
                routePath = "/comidas",
                entityName = "Comida",
                getAll = { services.comidaService.getAll() },
                read = { id -> services.comidaService.read(id) },
                create = { entity -> services.comidaService.create(entity as Comida) },
                update = { id, entity -> services.comidaService.update(id, entity as Comida) },
                delete = { id -> services.comidaService.delete(id) },
                receiveType = Comida::class
            )

            route("/objetivos") {
                get {
                    val userId = call.getUserId()
                    try {
                        val objetivos = if (userId != null) {
                            services.objetivoService.getAll(userId)
                        } else {
                            services.objetivoService.getAll()
                        }
                        call.respond(objetivos)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("Error al obtener objetivos: ${e.message}")
                        )
                    }
                }

                get("/{id}") {
                    validateId(call, "id") { id ->
                        try {
                            val objetivo = services.objetivoService.read(id)
                            if (objetivo != null) {
                                call.respond(objetivo)
                            } else {
                                call.respond(HttpStatusCode.NotFound, ErrorResponse("Objetivo no encontrado"))
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Error al leer objetivo: ${e.message}")
                            )
                        }
                    }
                }

                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@post
                    }

                    val objetivo = try {
                        call.receive<Objetivo>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Datos de objetivo inválidos: ${e.message}")
                        )
                        return@post
                    }

                    if (objetivo.usuarioId == null) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("usuarioId es requerido"))
                        return@post
                    }
                    if (objetivo.usuarioId != userId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("usuarioId no coincide con el usuario autenticado")
                        )
                        return@post
                    }

                    try {
                        val id = services.objetivoService.create(objetivo)
                        val createdObjetivo = services.objetivoService.read(id) ?: objetivo
                        call.respond(HttpStatusCode.Created, createdObjetivo)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse("No se pudo crear el objetivo: ${e.message}")
                        )
                    }
                }

                put("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()
                    validateId(call, "id") { id ->
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                            return@validateId
                        }

                        try {
                            val existingObjetivo = services.objetivoService.read(id)
                            if (existingObjetivo == null || existingObjetivo.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No tienes permiso para modificar este objetivo")
                                )
                                return@validateId
                            }

                            val updatedObjetivo = try {
                                call.receive<Objetivo>()
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ErrorResponse("Datos de objetivo inválidos: ${e.message}")
                                )
                                return@validateId
                            }

                            if (updatedObjetivo.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("usuarioId no coincide con el usuario autenticado")
                                )
                                return@validateId
                            }

                            services.objetivoService.update(id, updatedObjetivo)
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf("message" to "Objetivo actualizado correctamente")
                            )
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo actualizar el objetivo: ${e.message}")
                            )
                        }
                    }
                }

                delete("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asInt()
                    validateId(call, "id") { id ->
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                            return@validateId
                        }

                        try {
                            val existingObjetivo = services.objetivoService.read(id)
                            if (existingObjetivo == null || existingObjetivo.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No tienes permiso para eliminar este objetivo")
                                )
                                return@validateId
                            }

                            services.objetivoService.delete(id)
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf("message" to "Objetivo eliminado correctamente")
                            )
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo eliminar el objetivo: ${e.message}")
                            )
                        }
                    }
                }
            }

            route("/menus") {
                get {
                    val userId = call.getUserId()
                    try {
                        val menus = if (userId != null) {
                            services.menuService.getByUsuario(userId)
                        } else {
                            services.menuService.getAll()
                        }
                        call.respond(menus)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("Error al obtener menús: ${e.message}")
                        )
                    }
                }

                get("/{id}") {
                    validateId(call, "id") { id ->
                        try {
                            val menu = services.menuService.read(id)
                            if (menu != null) {
                                call.respond(menu)
                            } else {
                                call.respond(HttpStatusCode.NotFound, ErrorResponse("Menú no encontrado"))
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Error al leer menú: ${e.message}")
                            )
                        }
                    }
                }

                get("/{id}/ingredientes") {
                    validateId(call, "id") { id ->
                        try {
                            call.respond(services.seleccionIngredienteService.getSeleccionesConIngredientes(id))
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Error al obtener ingredientes del menú: ${e.message}")
                            )
                        }
                    }
                }

                post {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@post
                    }

                    val newMenu = try {
                        call.receive<Menu>()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("Datos de menú inválidos: ${e.message}")
                        )
                        return@post
                    }

                    if (newMenu.usuarioId != 0 && newMenu.usuarioId != userId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse("No puedes crear menús para otros usuarios")
                        )
                        return@post
                    }

                    try {
                        val menuToCreate = newMenu.copy(usuarioId = userId)
                        val id = services.menuService.create(menuToCreate)
                        val createdMenu = services.menuService.read(id) ?: menuToCreate
                        call.respond(HttpStatusCode.Created, createdMenu)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse("No se pudo crear el menú: ${e.message}")
                        )
                    }
                }

                put("/{id}") {
                    val userId = call.getUserId()
                    validateId(call, "id") { id ->
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                            return@validateId
                        }

                        try {
                            val existingMenu = services.menuService.read(id)
                            if (existingMenu == null || existingMenu.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No tienes permiso para modificar este menú")
                                )
                                return@validateId
                            }

                            val updatedMenu = call.receive<Menu>()
                            val menuToUpdate = updatedMenu.copy(usuarioId = userId)

                            val rowsAffected = services.menuService.update(id, menuToUpdate)
                            if (rowsAffected > 0) {
                                call.respond(
                                    HttpStatusCode.OK,
                                    mapOf("message" to "Menú actualizado correctamente")
                                )
                            } else {
                                call.respond(HttpStatusCode.NotFound, ErrorResponse("Menú no encontrado"))
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo actualizar el menú: ${e.message}")
                            )
                        }
                    }
                }

                delete("/{id}") {
                    val userId = call.getUserId()
                    validateId(call, "id") { id ->
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                            return@validateId
                        }

                        try {
                            val existingMenu = services.menuService.read(id)
                            if (existingMenu == null || existingMenu.usuarioId != userId) {
                                call.respond(
                                    HttpStatusCode.Forbidden,
                                    ErrorResponse("No tienes permiso para eliminar este menú")
                                )
                                return@validateId
                            }

                            val rowsAffected = services.menuService.delete(id)
                            if (rowsAffected > 0) {
                                call.respond(
                                    HttpStatusCode.OK,
                                    mapOf("message" to "Menú eliminado correctamente")
                                )
                            } else {
                                call.respond(HttpStatusCode.NotFound, ErrorResponse("Menú no encontrado"))
                            }
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("No se pudo eliminar el menú: ${e.message}")
                            )
                        }
                    }
                }

                route("/{menuId}/ingredientes") {
                    post {
                        val userId = call.getUserId()
                        validateId(call, "menuId") { menuId ->
                            if (userId == null) {
                                call.respond(
                                    HttpStatusCode.Unauthorized,
                                    ErrorResponse("Usuario no autenticado")
                                )
                                return@validateId
                            }

                            try {
                                val existingMenu = services.menuService.read(menuId)
                                if (existingMenu == null || existingMenu.usuarioId != userId) {
                                    call.respond(
                                        HttpStatusCode.Forbidden,
                                        ErrorResponse("No tienes permiso para modificar este menú")
                                    )
                                    return@validateId
                                }

                                val newSelection = call.receive<SeleccionIngrediente>()
                                val selectionToCreate = newSelection.copy(menuId = menuId)

                                val id = services.seleccionIngredienteService.create(selectionToCreate)
                                val createdSelection =
                                    services.seleccionIngredienteService.read(id) ?: selectionToCreate
                                call.respond(HttpStatusCode.Created, createdSelection)
                            } catch (e: Exception) {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    ErrorResponse("No se pudo agregar el ingrediente al menú: ${e.message}")
                                )
                            }
                        }
                    }

                    put("/{seleccionId}") {
                        val userId = call.getUserId()
                        validateId(call, "menuId") { menuId ->
                            validateId(call, "seleccionId") { seleccionId ->
                                if (userId == null) {
                                    call.respond(
                                        HttpStatusCode.Unauthorized,
                                        ErrorResponse("Usuario no autenticado")
                                    )
                                    return@validateId
                                }

                                try {
                                    val existingMenu = services.menuService.read(menuId)
                                    val existingSelection = services.seleccionIngredienteService.read(seleccionId)

                                    if (existingMenu == null || existingMenu.usuarioId != userId ||
                                        existingSelection == null || existingSelection.menuId != menuId
                                    ) {
                                        call.respond(
                                            HttpStatusCode.Forbidden,
                                            ErrorResponse("No tienes permiso para modificar este ingrediente del menú")
                                        )
                                        return@validateId
                                    }

                                    val updatedSelection = call.receive<SeleccionIngrediente>()
                                    val selectionToUpdate = updatedSelection.copy(
                                        menuId = menuId,
                                        seleccionId = seleccionId
                                    )

                                    val rowsAffected = services.seleccionIngredienteService.update(
                                        seleccionId,
                                        selectionToUpdate
                                    )
                                    if (rowsAffected > 0) {
                                        call.respond(
                                            HttpStatusCode.OK,
                                            mapOf("message" to "Ingrediente del menú actualizado correctamente")
                                        )
                                    } else {
                                        call.respond(
                                            HttpStatusCode.NotFound,
                                            ErrorResponse("Selección de ingrediente no encontrada")
                                        )
                                    }
                                } catch (e: Exception) {
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        ErrorResponse("No se pudo actualizar el ingrediente del menú: ${e.message}")
                                    )
                                }
                            }
                        }
                    }

                    delete("/{seleccionId}") {
                        val userId = call.getUserId()
                        validateId(call, "menuId") { menuId ->
                            validateId(call, "seleccionId") { seleccionId ->
                                if (userId == null) {
                                    call.respond(
                                        HttpStatusCode.Unauthorized,
                                        ErrorResponse("Usuario no autenticado")
                                    )
                                    return@validateId
                                }

                                try {
                                    val existingMenu = services.menuService.read(menuId)
                                    val existingSelection = services.seleccionIngredienteService.read(seleccionId)

                                    if (existingMenu == null || existingMenu.usuarioId != userId ||
                                        existingSelection == null || existingSelection.menuId != menuId
                                    ) {
                                        call.respond(
                                            HttpStatusCode.Forbidden,
                                            ErrorResponse("No tienes permiso para eliminar este ingrediente del menú")
                                        )
                                        return@validateId
                                    }

                                    val rowsAffected = services.seleccionIngredienteService.delete(seleccionId)
                                    if (rowsAffected > 0) {
                                        call.respond(
                                            HttpStatusCode.OK,
                                            mapOf("message" to "Ingrediente eliminado del menú correctamente")
                                        )
                                    } else {
                                        call.respond(
                                            HttpStatusCode.NotFound,
                                            ErrorResponse("Selección de ingrediente no encontrada")
                                        )
                                    }
                                } catch (e: Exception) {
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        ErrorResponse("No se pudo eliminar el ingrediente del menú: ${e.message}")
                                    )
                                }
                            }
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
                call.respond(services.ingredienteService.getAll())
            }

            get("/metodos") {
                call.respond(services.metodoPreparacionService.getAll())
            }

            get("/comidas") {
                call.respond(services.comidaService.getAll())
            }

            get("/objetivos") {
                try {
                    call.respond(services.objetivoService.getAll())
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Error al obtener objetivos: ${e.message}")
                    )
                }
            }
        }
    }
}