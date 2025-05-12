package com.christian.nutriplan.services

import com.christian.nutriplan.models.RecetaIngrediente
import com.christian.nutriplan.models.RecetaIngredientes
import com.christian.nutriplan.models.Ingredientes // Asegúrate de tener esta importación
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class RecetaIngredienteService(private val database: Database) {
    fun create(recetaIngrediente: RecetaIngrediente) = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            RecetaIngredientes.insert {
                it[recetaId] = recetaIngrediente.recetaId
                it[ingredienteId] = recetaIngrediente.ingredienteId
                it[cantidad] = recetaIngrediente.cantidad?.toBigDecimal()
                it[unidad] = recetaIngrediente.unidad
            }
            println("Relación receta-ingrediente creada: recetaId=${recetaIngrediente.recetaId}, ingredienteId=${recetaIngrediente.ingredienteId}")
        } catch (e: Exception) {
            println("Error al crear relación receta-ingrediente: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(recetaId: Int, ingredienteId: Int): RecetaIngrediente? = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetaIngredientes innerJoin Ingredientes)
            .selectAll().where { (RecetaIngredientes.recetaId eq recetaId) and (RecetaIngredientes.ingredienteId eq ingredienteId) }
            .map {
                RecetaIngrediente(
                    recetaId = it[RecetaIngredientes.recetaId],
                    ingredienteId = it[RecetaIngredientes.ingredienteId],
                    nombreIngrediente = it[Ingredientes.nombre],
                    cantidad = it[RecetaIngredientes.cantidad]?.toDouble(),
                    unidad = it[RecetaIngredientes.unidad]
                )
            }.singleOrNull().also {
                println("Relación encontrada para recetaId=$recetaId, ingredienteId=$ingredienteId: ${it != null}")
            }
    }

    fun update(recetaId: Int, ingredienteId: Int, recetaIngrediente: RecetaIngrediente) = transaction(database) {
        addLogger(StdOutSqlLogger)
        RecetaIngredientes.update({
            (RecetaIngredientes.recetaId eq recetaId) and (RecetaIngredientes.ingredienteId eq ingredienteId)
        }) {
            it[cantidad] = recetaIngrediente.cantidad?.toBigDecimal()
            it[unidad] = recetaIngrediente.unidad
        }.also {
            println("Relación actualizada para recetaId=$recetaId, ingredienteId=$ingredienteId: $it filas afectadas")
        }
    }

    fun delete(recetaId: Int, ingredienteId: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        RecetaIngredientes.deleteWhere {
            (RecetaIngredientes.recetaId eq recetaId) and (RecetaIngredientes.ingredienteId eq ingredienteId)
        }.also {
            println("Relación eliminada para recetaId=$recetaId, ingredienteId=$ingredienteId: $it filas afectadas")
        }
    }

    fun getAll(): List<RecetaIngrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetaIngredientes innerJoin Ingredientes)
            .selectAll()
            .map {
                RecetaIngrediente(
                    recetaId = it[RecetaIngredientes.recetaId],
                    ingredienteId = it[RecetaIngredientes.ingredienteId],
                    nombreIngrediente = it[Ingredientes.nombre],
                    cantidad = it[RecetaIngredientes.cantidad]?.toDouble(),
                    unidad = it[RecetaIngredientes.unidad]
                )
            }.also {
                println("Total relaciones receta-ingrediente obtenidas: ${it.size}")
            }
    }

    fun getByReceta(recetaId: Int): List<RecetaIngrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetaIngredientes innerJoin Ingredientes)
            .select(
                RecetaIngredientes.recetaId,
                RecetaIngredientes.ingredienteId,
                Ingredientes.nombre,
                RecetaIngredientes.cantidad,
                RecetaIngredientes.unidad
            )
            .where { RecetaIngredientes.recetaId eq recetaId }
            .map {
                RecetaIngrediente(
                    recetaId = it[RecetaIngredientes.recetaId],
                    ingredienteId = it[RecetaIngredientes.ingredienteId],
                    nombreIngrediente = it[Ingredientes.nombre],
                    cantidad = it[RecetaIngredientes.cantidad]?.toDouble(),
                    unidad = it[RecetaIngredientes.unidad]
                )
            }.also {
                println("Relaciones encontradas para recetaId=$recetaId: ${it.size}")
            }
    }

    fun getRecetasIngredientesPaginated(page: Int, pageSize: Int): List<RecetaIngrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetaIngredientes innerJoin Ingredientes)
            .selectAll()
            .limit(pageSize).offset((page - 1) * pageSize.toLong())
            .map {
                RecetaIngrediente(
                    recetaId = it[RecetaIngredientes.recetaId],
                    ingredienteId = it[RecetaIngredientes.ingredienteId],
                    nombreIngrediente = it[Ingredientes.nombre],
                    cantidad = it[RecetaIngredientes.cantidad]?.toDouble(),
                    unidad = it[RecetaIngredientes.unidad]
                )
            }.also {
                println("Relaciones obtenidas para página $page (tamaño $pageSize): ${it.size}")
            }
    }
}