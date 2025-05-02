// src/main/kotlin/com/christian/nutriplan/plugins/Routing.kt
package com.christian.nutriplan.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.christian.nutriplan.models.*
import kotlin.reflect.KClass

fun Application.configureRouting(services: Services) {
    // Centralized error response data class
    data class ErrorResponse(val error: String)

    // Helper function to validate ID and execute a block
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

    // Helper function to handle CRUD operations for entities
    fun Route.handleCrud(
        routePath: String,
        entityName: String,
        getAll: suspend () -> List<Any>,
        read: suspend (Int) -> Any?,
        create: suspend (Any) -> Int,
        update: suspend (Int, Any) -> Unit,
        delete: suspend (Int) -> Int,
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
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("No se pudo crear el $entityName: ${e.message}"))
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
                    val result = delete(id)
                    if (result > 0) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "$entityName eliminado correctamente"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("$entityName no encontrado"))
                    }
                }
            }
        }
    }

    // Helper function to get authenticated user ID
    fun ApplicationCall.getUserId(): Int? {
        val principal = principal<JWTPrincipal>()
        return principal?.payload?.getClaim("userId")?.asInt()
    }

    routing {
        // Base route
        get("/") {
            call.respondText("API de NutriPlan - Sistema de Planificación Nutricional")
        }

        // Rutas para gestión de usuarios (solo accesibles por administradores)
        route("/usuarios") {
            get {


                val usuarios = services.usuarioService.getAllUsers()
                call.respond(usuarios)
            }

            get("/{id}") {
                validateId(call, "id") { id ->
                    val usuario = services.usuarioService.read(id)
                    if (usuario != null) {
                        // Omitir la contraseña en la respuesta
                        val response = UsuarioResponse(
                            usuarioId = usuario.usuarioId ?: throw IllegalStateException("ID de usuario no puede ser nulo"),
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

            // Opcional: Endpoint paginado
            get("/paginated") {

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10
                val usuarios = services.usuarioService.getUsersPaginated(page, size)
                call.respond(usuarios)
            }
        }


        // Protected routes requiring JWT authentication
        authenticate("auth-jwt") {
            // CRUD routes for CategoriaIngrediente
            handleCrud(
                routePath = "/categorias",
                entityName = "Categoría",
                getAll = { services.categoriaIngredienteService.getAll() },
                read = { id -> services.categoriaIngredienteService.read(id) },
                create = { entity -> services.categoriaIngredienteService.create(entity as CategoriaIngrediente) },
                update = { id, entity -> services.categoriaIngredienteService.update(id, entity as CategoriaIngrediente) },
                delete = { id -> services.categoriaIngredienteService.delete(id) },
                receiveType = CategoriaIngrediente::class
            )






            // CRUD routes for Ingrediente with additional search endpoints
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

            // CRUD routes for MetodoPreparacion
            handleCrud(
                routePath = "/metodos",
                entityName = "Método",
                getAll = { services.metodoPreparacionService.getAll() },
                read = { id -> services.metodoPreparacionService.read(id) },
                create = { entity -> services.metodoPreparacionService.create(entity as MetodoPreparacion) },
                update = { id, entity -> services.metodoPreparacionService.update(id, entity as MetodoPreparacion) },
                delete = { id -> services.metodoPreparacionService.delete(id) },
                receiveType = MetodoPreparacion::class
            )

            // CRUD routes for Comida
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

            // CRUD routes for Objetivo
            handleCrud(
                routePath = "/objetivos",
                entityName = "Objetivo",
                getAll = { services.objetivoService.getAll() },
                read = { id -> services.objetivoService.read(id) },
                create = { entity -> services.objetivoService.create(entity as Objetivo) },
                update = { id, entity -> services.objetivoService.update(id, entity as Objetivo) },
                delete = { id -> services.objetivoService.delete(id) },
                receiveType = Objetivo::class
            )

            // Menu routes with user-specific logic
            route("/menus") {
                get {
                    val userId = call.getUserId()
                    if (userId != null) {
                        call.respond(services.menuService.getByUsuario(userId))
                    } else {
                        call.respond(services.menuService.getAll())
                    }
                }

                get("/{id}") {
                    validateId(call, "id") { id ->
                        val menu = services.menuService.read(id)
                        if (menu != null) {
                            call.respond(menu)
                        } else {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Menú no encontrado"))
                        }
                    }
                }

                get("/{id}/ingredientes") {
                    validateId(call, "id") { id ->
                        call.respond(services.seleccionIngredienteService.getSeleccionesConIngredientes(id))
                    }
                }

                post {
                    val userId = call.getUserId()
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                        return@post
                    }

                    val newMenu = call.receive<Menu>()
                    if (newMenu.usuarioId != 0 && newMenu.usuarioId != userId) {
                        call.respond(HttpStatusCode.Forbidden, ErrorResponse("No puedes crear menús para otros usuarios"))
                        return@post
                    }

                    try {
                        val menuToCreate = newMenu.copy(usuarioId = userId)
                        val id = services.menuService.create(menuToCreate)
                        val createdMenu = services.menuService.read(id) ?: menuToCreate
                        call.respond(HttpStatusCode.Created, createdMenu)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.Conflict, ErrorResponse("No se pudo crear el menú: ${e.message}"))
                    }
                }

                put("/{id}") {
                    val userId = call.getUserId()
                    validateId(call, "id") { id ->
                        if (userId == null) {
                            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                            return@validateId
                        }

                        val existingMenu = services.menuService.read(id)
                        if (existingMenu == null || existingMenu.usuarioId != userId) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse("No tienes permiso para modificar este menú"))
                            return@validateId
                        }

                        val updatedMenu = call.receive<Menu>()
                        val menuToUpdate = updatedMenu.copy(usuarioId = userId)

                        try {
                            services.menuService.update(id, menuToUpdate)
                            call.respond(HttpStatusCode.OK, mapOf("message" to "Menú actualizado correctamente"))
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

                        val existingMenu = services.menuService.read(id)
                        if (existingMenu == null || existingMenu.usuarioId != userId) {
                            call.respond(HttpStatusCode.Forbidden, ErrorResponse("No tienes permiso para eliminar este menú"))
                            return@validateId
                        }

                        val result = services.menuService.delete(id)
                        if (result > 0) {
                            call.respond(HttpStatusCode.OK, mapOf("message" to "Menú eliminado correctamente"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Menú no encontrado"))
                        }
                    }
                }

                // Routes for managing ingredients in a menu
                route("/{menuId}/ingredientes") {
                    post {
                        val userId = call.getUserId()
                        validateId(call, "menuId") { menuId ->
                            if (userId == null) {
                                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                                return@validateId
                            }

                            val existingMenu = services.menuService.read(menuId)
                            if (existingMenu == null || existingMenu.usuarioId != userId) {
                                call.respond(HttpStatusCode.Forbidden, ErrorResponse("No tienes permiso para modificar este menú"))
                                return@validateId
                            }

                            val newSelection = call.receive<SeleccionIngrediente>()
                            val selectionToCreate = newSelection.copy(menuId = menuId)

                            try {
                                val id = services.seleccionIngredienteService.create(selectionToCreate)
                                val createdSelection = services.seleccionIngredienteService.read(id) ?: selectionToCreate
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
                                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                                    return@validateId
                                }

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
                                val selectionToUpdate = updatedSelection.copy(menuId = menuId, seleccionId = seleccionId)

                                try {
                                    services.seleccionIngredienteService.update(seleccionId, selectionToUpdate)
                                    call.respond(
                                        HttpStatusCode.OK,
                                        mapOf("message" to "Ingrediente del menú actualizado correctamente")
                                    )
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
                                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Usuario no autenticado"))
                                    return@validateId
                                }

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

                                val result = services.seleccionIngredienteService.delete(seleccionId)
                                if (result > 0) {
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
                            }
                        }
                    }
                }
            }
        }

        // Public routes (no authentication required)
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
                call.respond(services.objetivoService.getAll())
            }
        }
    }
}