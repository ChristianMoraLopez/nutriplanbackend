package com.christian.nutriplan.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Data models and database tables for NutriPlan nutritional planning system
 */

// User models
@Serializable
data class Usuario(
    val usuarioId: Int? = null,  // Cambio aquí: usa Int en lugar de EntityID
    val nombre: String,
    val email: String,
    val contrasena: String,
    val aceptaTerminos: Boolean = false,
    val rol: String = "usuario",
    @Contextual
    val fechaRegistro: String = LocalDateTime.now().toString()
)
@Serializable
data class UsuarioResponse(
    val usuarioId: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val fechaRegistro: String,
    val aceptaTerminos: Boolean
)

// Authentication model
@Serializable
data class Credentials(
    val email: String,
    val password: String
)

// Nutrition models
@Serializable
data class CategoriaIngrediente(
    val categoriaId: Int? = null,
    val nombre: String
)

@Serializable
data class Ingrediente(
    val ingredienteId: Int? = null,
    val nombre: String,
    val categoriaId: Int,
    val calorias: Double? = null
)

@Serializable
data class MetodoPreparacion(
    val metodoId: Int? = null,
    val nombre: String,
    val descripcion: String? = null
)

@Serializable
data class Comida(
    val comidaId: Int? = null,
    val nombre: String
)

@Serializable
data class Objetivo(
    val objetivoId: Int? = null,
    val nombre: String,
    val tieneTiempo: Boolean
)

// Menu planning models
@Serializable
data class Menu(
    val menuId: Int? = null,
    val usuarioId: Int,
    val objetivoId: Int,
    val comidaId: Int,
    @Contextual
    val fechaCreacion: String? = LocalDateTime.now().toString(),
    val metodoId: Int? = null
)

@Serializable
data class SeleccionIngrediente(
    val seleccionId: Int? = null,
    val menuId: Int,
    val ingredienteId: Int,
    val cantidad: Double? = null
)

// Exposed ORM Table Definitions

object Usuarios : IdTable<Int>("usuarios") {
    override val id: Column<EntityID<Int>> = integer("usuario_id").autoIncrement().entityId()
    val nombre = varchar("nombre", length = 255)
    val email = varchar("email", length = 255).uniqueIndex()
    val contrasena = varchar("contrasena", length = 255)
    val aceptaTerminos = bool("acepta_terminos").default(false)
    val rol = varchar("rol", length = 50).default("usuario")
    val fechaRegistro = datetime("fecha_registro").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id, name = "PK_Usuario_ID")
}

object CategoriasIngredientes : Table("categorias_ingredientes") {
    val categoriaId = integer("categoria_id").autoIncrement()
    val nombre = varchar("nombre", length = 50).uniqueIndex()

    override val primaryKey = PrimaryKey(categoriaId)
}

object Ingredientes : Table("ingredientes") {
    val ingredienteId = integer("ingrediente_id").autoIncrement()
    val nombre = varchar("nombre", length = 100)
    val categoriaId = integer("categoria_id") references CategoriasIngredientes.categoriaId
    val calorias = decimal("calorias", precision = 10, scale = 2).nullable()

    override val primaryKey = PrimaryKey(ingredienteId)
}

object MetodosPreparacion : Table("metodos_preparacion") {
    val metodoId = integer("metodo_id").autoIncrement()
    val nombre = varchar("nombre", length = 100)
    val descripcion = text("descripcion").nullable()

    override val primaryKey = PrimaryKey(metodoId)
}

object Comidas : Table("comidas") {
    val comidaId = integer("comida_id").autoIncrement()
    val nombre = varchar("nombre", length = 50).uniqueIndex()

    override val primaryKey = PrimaryKey(comidaId)
}

object Objetivos : Table("objetivos") {
    val objetivoId = integer("objetivo_id").autoIncrement()
    val nombre = varchar("nombre", length = 100)
    val tieneTiempo = bool("tiene_tiempo")

    override val primaryKey = PrimaryKey(objetivoId)
}

object Menus : Table("menus") {
    val menuId = integer("menu_id").autoIncrement()
    val usuarioId = reference("usuario_id", Usuarios) // <-- Cambiado aquí

    val objetivoId = integer("objetivo_id").references(Objetivos.objetivoId)
    val comidaId = integer("comida_id").references(Comidas.comidaId)
    val fechaCreacion = datetime("fecha_creacion").clientDefault { LocalDateTime.now() }
    val metodoId = integer("metodo_id").references(MetodosPreparacion.metodoId).nullable()

    override val primaryKey = PrimaryKey(menuId, name = "PK_Menu_ID")
}

object SeleccionIngredientes : Table("seleccion_ingredientes") {
    val seleccionId = integer("seleccion_id").autoIncrement()
    val menuId = integer("menu_id") references Menus.menuId
    val ingredienteId = integer("ingrediente_id") references Ingredientes.ingredienteId
    val cantidad = decimal("cantidad", precision = 10, scale = 2).nullable()

    override val primaryKey = PrimaryKey(seleccionId)
}

// Current timestamp helper
object CurrentDateTime {
    operator fun invoke() = LocalDateTime.now()
}