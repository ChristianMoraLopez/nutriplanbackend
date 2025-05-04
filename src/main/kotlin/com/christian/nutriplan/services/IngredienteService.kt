package com.christian.nutriplan.services

import com.christian.nutriplan.models.Ingrediente
import com.christian.nutriplan.models.Ingredientes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class IngredienteService(private val database: Database) {
    fun create(ingrediente: Ingrediente): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val ingredienteId = Ingredientes.insert {
                it[nombre] = ingrediente.nombre
                it[categoriaId] = ingrediente.categoriaId
                it[calorias] = ingrediente.calorias
            }[Ingredientes.ingredienteId]

            println("Ingrediente creado con ID: $ingredienteId")
            return@transaction ingredienteId
        } catch (e: Exception) {
            println("Error al crear ingrediente: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): Ingrediente? = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.select ( Ingredientes.ingredienteId eq id )
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }.singleOrNull().also {
                println("Ingrediente encontrado para id $id: ${it != null}")
            }
    }

    fun update(id: Int, ingrediente: Ingrediente) = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val rowsUpdated = Ingredientes.update({ Ingredientes.ingredienteId eq id }) {
                it[nombre] = ingrediente.nombre
                it[categoriaId] = ingrediente.categoriaId
                it[calorias] = ingrediente.calorias
            }
            println("Ingrediente actualizado: $rowsUpdated filas afectadas")
        } catch (e: Exception) {
            println("Error al actualizar ingrediente: ${e.message}")
            throw e
        }
    }

    fun delete(id: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val rowsDeleted = Ingredientes.deleteWhere { Ingredientes.ingredienteId eq id }
            println("Ingrediente eliminado: $rowsDeleted filas afectadas")
        } catch (e: Exception) {
            println("Error al eliminar ingrediente: ${e.message}")
            throw e
        }
    }

    fun getAll(): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll()
            .orderBy(Ingredientes.nombre to SortOrder.ASC)
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }.also {
                println("Total ingredientes obtenidos: ${it.size}")
            }
    }

    fun searchByName(query: String): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.select ( Ingredientes.nombre like "%${query.trim()}%" )
            .orderBy(Ingredientes.nombre to SortOrder.ASC)
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }.also {
                println("Ingredientes encontrados para '$query': ${it.size}")
            }
    }

    fun getByCategoria(categoriaId: Int): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.select ( Ingredientes.categoriaId eq categoriaId )
            .orderBy(Ingredientes.nombre to SortOrder.ASC)
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }.also {
                println("Ingredientes encontrados para categoría $categoriaId: ${it.size}")
            }
    }

    fun getPaginated(page: Int, pageSize: Int): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll()
            .orderBy(Ingredientes.nombre to SortOrder.ASC)
            .limit(pageSize)
            .offset((page - 1) * pageSize.toLong())
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }.also {
                println("Ingredientes obtenidos (página $page, tamaño $pageSize): ${it.size}")
            }
    }
}