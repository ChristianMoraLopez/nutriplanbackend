package com.christian.nutriplan.models

import com.christian.nutriplan.models.Usuarios.defaultValue
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.Instant
import java.time.LocalDateTime

/**
 * Data models and database tables for NutriPlan nutritional planning system
 */

// User models
@Serializable
data class Usuario(
    val usuarioId: Int? = null,
    val nombre: String,
    val email: String,
    val contrasena: String,
    val aceptaTerminos: Boolean = false,
    val rol: String = "usuario",
    @Contextual
    val fechaRegistro: String = LocalDateTime.now().toString(),
    val ciudad: String,
    val localidad: String

)


@Serializable
data class UsuarioResponse(
    val usuarioId: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    @Contextual
    val fechaRegistro: String,
    val aceptaTerminos: Boolean
)

// Authentication model
@Serializable
data class Credentials(
    val email: String,
    val contrasena: String
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
    val calorias: String? = null,
    val fit: Boolean = false,
    val disponibleBogota: Boolean = true,
    val fotografia: String? = null
)

@Serializable
data class MetodoPreparacion(
    val metodoId: Int? = null,
    val nombre: String,
    val descripcion: String? = null
)

@Serializable
data class TipoComida(
    val tipoComidaId: Int? = null,
    val nombre: String
)
@Serializable
data class Receta(
    val recetaId: Int? = null,
    val nombre: String,
    val tipoComidaId: Int,
    val fit: Boolean = false,
    val instrucciones: String,
    val tiempoPreparacion: Int? = null,
    val disponibleBogota: Boolean = true,
    val metodoId: Int? = null
)

@Serializable
data class RecetaIngrediente(
    val recetaId: Int,
    val ingredienteId: Int,
    val nombreIngrediente: String? = null,
    val cantidad: Double? = null,
    val unidad: String? = null
)

@Serializable
data class RecetaGuardada(
    val guardadoId: Int? = null,
    val usuarioId: Int,
    val recetaId: Int,
    @Contextual
    val fechaGuardado: String = LocalDateTime.now().toString(),
    val comentarioPersonal: String? = null,
    val nombreReceta: String? = null
)

@Serializable
data class ErrorResponse(
    val message: String,
    val error: String
)

// Exposed ORM Table Definitions

object Usuarios : IdTable<Int>("usuarios") {
    override val id: Column<EntityID<Int>> = integer("usuario_id").autoIncrement().entityId()
    val nombre = varchar("nombre", length = 255)
    val email = varchar("email", length = 255).uniqueIndex()
    val contrasena = varchar("contrasena", length = 255)
    val aceptaTerminos = bool("acepta_terminos").default(false)
    val rol = varchar("rol", length = 50).default("usuario")
    val defaultValue: () -> LocalDateTime = { LocalDateTime.now() }
    val fechaRegistro = datetime("fecha_registro").clientDefault(defaultValue)
    val ciudad = varchar("ciudad", length = 100).default("")
    val localidad = varchar("localidad", length = 100).default("")

    override val primaryKey = PrimaryKey(id, name = "PK_Usuario_ID")
}

object CategoriasIngredientes : IdTable<Int>("categorias_ingredientes") {
    override val id: Column<EntityID<Int>> = integer("categoria_id").autoIncrement().entityId()
    val nombre = varchar("nombre", 50)

    override val primaryKey = PrimaryKey(id, name = "categorias_ingredientes_pkey")
}

object Ingredientes : IdTable<Int>("ingredientes") {
    override val id: Column<EntityID<Int>> = integer("ingrediente_id").autoIncrement().entityId()
    val nombre = varchar("nombre", 100)
    val categoriaId = integer("categoria_id").references(CategoriasIngredientes.id)
    val calorias = varchar("calorias", 100).default("None")
    val fit = bool("fit").default(false)
    val disponibleBogota = bool("disponible_bogota").default(true)
    val fotografia = varchar("fotografia", 255).nullable()

    override val primaryKey = PrimaryKey(id, name = "ingredientes_pkey")
}

object MetodosPreparacion : IdTable<Int>("metodos_preparacion") {
    override val id: Column<EntityID<Int>> = integer("metodo_id").autoIncrement().entityId()
    val nombre = varchar("nombre", 100)
    val descripcion = text("descripcion").nullable()

    override val primaryKey = PrimaryKey(id, name = "metodos_preparacion_pkey")
}


object TiposComida : IdTable<Int>("tipos_comida") {
    override val id: Column<EntityID<Int>> = integer("tipo_comida_id").autoIncrement().entityId()
    val nombre = varchar("nombre", 50)

    override val primaryKey = PrimaryKey(id, name = "tipos_comida_pkey")
}




object Recetas : IdTable<Int>("recetas") {
    override val id: Column<EntityID<Int>> = integer("receta_id").autoIncrement().entityId()
    val nombre = varchar("nombre", 100)
    val tipoComidaId = integer("tipo_comida_id").references(TiposComida.id)
    val fit = bool("fit").default(false)
    val instrucciones = text("instrucciones")
    val tiempoPreparacion = integer("tiempo_preparacion").nullable()
    val disponibleBogota = bool("disponible_bogota").default(true)
    val metodoId = integer("metodo_id").references(MetodosPreparacion.id).nullable()

    override val primaryKey = PrimaryKey(id, name = "recetas_pkey")
}

object RecetaIngredientes : Table("receta_ingredientes") {
    val recetaId = integer("receta_id").references(Recetas.id)
    val ingredienteId = integer("ingrediente_id").references(Ingredientes.id)
    val nombreIngrediente = varchar("nombre_ingrediente", 50)
    val cantidad = decimal("cantidad", precision = 10, scale = 2).nullable()
    val unidad = varchar("unidad", 50).nullable()

    override val primaryKey = PrimaryKey(recetaId, ingredienteId, name = "receta_ingredientes_pkey")
}

object RecetasGuardadas : IdTable<Int>("recetas_guardadas") {
    override val id: Column<EntityID<Int>> = integer("guardado_id").autoIncrement().entityId()
    val usuarioId = integer("usuario_id").references(Usuarios.id)
    val recetaId = integer("receta_id").references(Recetas.id)
    val fechaGuardado = datetime("fecha_guardado").clientDefault(defaultValue)
    val comentarioPersonal = text("comentario_personal").nullable()

    override val primaryKey = PrimaryKey(id, name = "recetas_guardadas_pkey")
}

// Current timestamp helper
object CurrentDateTime {
    operator fun invoke() = Instant.now()
}