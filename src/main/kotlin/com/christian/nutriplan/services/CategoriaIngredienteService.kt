package com.christian.nutriplan.services

import com.christian.nutriplan.models.CategoriaIngrediente
import com.christian.nutriplan.models.CategoriasIngredientes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

class CategoriaIngredienteService(private val database: Database) {

    fun create(categoria: CategoriaIngrediente): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val categoriaId = CategoriasIngredientes.insertAndGetId {
                it[nombre] = categoria.nombre
            }.value

            println("Categoría creada con ID: $categoriaId")
            return@transaction categoriaId
        } catch (e: Exception) {
            println("Error al crear categoría: ${e.message}")
            throw e
        }
    }

    fun read(id: Int): CategoriaIngrediente? = transaction(database) {
        addLogger(StdOutSqlLogger)
        CategoriasIngredientes.selectAll().where { CategoriasIngredientes.id eq id }.map {
            CategoriaIngrediente(
                categoriaId = it[CategoriasIngredientes.id].value,
                nombre = it[CategoriasIngredientes.nombre]
            )
        }.singleOrNull().also {
            println("Categoría encontrada para id $id: ${it != null}")
        }
    }

    fun update(id: Int, categoria: CategoriaIngrediente): Boolean = transaction(database) {
        addLogger(StdOutSqlLogger)
        val rowsUpdated = CategoriasIngredientes.update({ CategoriasIngredientes.id eq id }) {
            it[nombre] = categoria.nombre
        }
        println("Categoría actualizada para id $id: $rowsUpdated filas afectadas")
        rowsUpdated > 0
    }

    fun delete(id: Int): Boolean = transaction(database) {
        addLogger(StdOutSqlLogger)
        val rowsDeleted = CategoriasIngredientes.deleteWhere { CategoriasIngredientes.id eq id }
        println("Categoría eliminada para id $id: $rowsDeleted filas afectadas")
        rowsDeleted > 0
    }

    fun getAll(): List<CategoriaIngrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        CategoriasIngredientes.selectAll().map {
            CategoriaIngrediente(
                categoriaId = it[CategoriasIngredientes.id].value,
                nombre = it[CategoriasIngredientes.nombre]
            )
        }.also {
            println("Total categorías obtenidas: ${it.size}")
        }
    }

    fun getPaginated(page: Int, pageSize: Int): List<CategoriaIngrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        CategoriasIngredientes.selectAll()
            .limit(pageSize).offset(start = ((page - 1) * pageSize).toLong())
            .map {
                CategoriaIngrediente(
                    categoriaId = it[CategoriasIngredientes.id].value,
                    nombre = it[CategoriasIngredientes.nombre]
                )
            }.also {
                println("Categorías obtenidas para página $page (tamaño $pageSize): ${it.size}")
            }
    }

    fun searchByName(query: String): List<CategoriaIngrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        CategoriasIngredientes.selectAll().where { CategoriasIngredientes.nombre like "%${query}%" }.map {
            CategoriaIngrediente(
                categoriaId = it[CategoriasIngredientes.id].value,
                nombre = it[CategoriasIngredientes.nombre]
            )
        }.also {
            println("Búsqueda de categorías por nombre '$query': ${it.size} resultados")
        }
    }
}