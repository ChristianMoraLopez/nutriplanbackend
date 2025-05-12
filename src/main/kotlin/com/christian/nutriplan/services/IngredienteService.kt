package com.christian.nutriplan.services

import com.christian.nutriplan.models.Ingrediente
import com.christian.nutriplan.models.Ingredientes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like


class IngredienteService(private val database: Database) {

    fun create(ingrediente: Ingrediente): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val ingredienteId = Ingredientes.insertAndGetId {
                it[nombre] = ingrediente.nombre
                it[categoriaId] = ingrediente.categoriaId
                it[calorias] = ingrediente.calorias ?: "None"
                it[fit] = ingrediente.fit
                it[disponibleBogota] = ingrediente.disponibleBogota
                it[fotografia] = ingrediente.fotografia
            }.value

            println("Ingrediente creado con ID: $ingredienteId")
            return@transaction ingredienteId
        } catch (e: Exception) {
            println("Error al crear ingrediente: ${e.message}")
            throw e
        }
    }


    fun read(id: Int): Ingrediente? = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll().where { Ingredientes.id eq id }.map {
            Ingrediente(
                ingredienteId = it[Ingredientes.id].value,
                nombre = it[Ingredientes.nombre],
                categoriaId = it[Ingredientes.categoriaId],
                calorias = it[Ingredientes.calorias],
                fit = it[Ingredientes.fit],
                disponibleBogota = it[Ingredientes.disponibleBogota],
                fotografia = it[Ingredientes.fotografia]
            )
        }.singleOrNull().also {
            println("Ingrediente encontrado para id $id: ${it != null}")
        }
    }

    fun update(id: Int, ingrediente: Ingrediente): Boolean = transaction(database) {
        addLogger(StdOutSqlLogger)
        val rowsUpdated = Ingredientes.update({ Ingredientes.id eq id }) {
            it[nombre] = ingrediente.nombre
            it[categoriaId] = ingrediente.categoriaId
            it[calorias] = ingrediente.calorias ?: "None"
            it[fit] = ingrediente.fit
            it[disponibleBogota] = ingrediente.disponibleBogota
            it[fotografia] = ingrediente.fotografia
        }
        println("Ingrediente actualizado para id $id: $rowsUpdated filas afectadas")
        rowsUpdated > 0
    }

    fun delete(id: Int): Boolean = transaction(database) {
        addLogger(StdOutSqlLogger)
        val rowsDeleted = Ingredientes.deleteWhere { Ingredientes.id eq id }
        println("Ingrediente eliminado para id $id: $rowsDeleted filas afectadas")
        rowsDeleted > 0
    }

    fun getAllIngredientes(): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll().map {
            Ingrediente(
                ingredienteId = it[Ingredientes.id].value,
                nombre = it[Ingredientes.nombre],
                categoriaId = it[Ingredientes.categoriaId],
                calorias = it[Ingredientes.calorias],
                fit = it[Ingredientes.fit],
                disponibleBogota = it[Ingredientes.disponibleBogota],
                fotografia = it[Ingredientes.fotografia]
            )
        }.also {
            println("Total ingredientes obtenidos: ${it.size}")
        }
    }

    fun getIngredientesPaginated(page: Int, pageSize: Int): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll()
            .limit(pageSize).offset(start = ((page - 1) * pageSize).toLong())
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.id].value,
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias],
                    fit = it[Ingredientes.fit],
                    disponibleBogota = it[Ingredientes.disponibleBogota],
                    fotografia = it[Ingredientes.fotografia]
                )
            }.also {
                println("Ingredientes obtenidos para página $page (tamaño $pageSize): ${it.size}")
            }
    }

    fun searchByName(query: String): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll().where {Ingredientes.nombre.lowerCase() eq "%${query.lowercase()}%"}.map {
            Ingrediente(
                ingredienteId = it[Ingredientes.id].value,
                nombre = it[Ingredientes.nombre],
                categoriaId = it[Ingredientes.categoriaId],
                calorias = it[Ingredientes.calorias],
                fit = it[Ingredientes.fit],
                disponibleBogota = it[Ingredientes.disponibleBogota],
                fotografia = it[Ingredientes.fotografia]
            )
        }.also {
            println("Búsqueda de ingredientes por nombre '$query': ${it.size} resultados")
        }
    }
    fun getByCategoria(categoriaId: Int): List<Ingrediente> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Ingredientes.selectAll().where {Ingredientes.categoriaId eq categoriaId }.map {
            Ingrediente(
                ingredienteId = it[Ingredientes.id].value,
                nombre = it[Ingredientes.nombre],
                categoriaId = it[Ingredientes.categoriaId],
                calorias = it[Ingredientes.calorias],
                fit = it[Ingredientes.fit],
                disponibleBogota = it[Ingredientes.disponibleBogota],
                fotografia = it[Ingredientes.fotografia]
            )
        }.also {
            println("Ingredientes obtenidos para categoría $categoriaId: ${it.size}")
        }
    }
}