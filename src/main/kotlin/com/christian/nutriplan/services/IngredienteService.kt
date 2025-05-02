package com.christian.nutriplan.services

import com.christian.nutriplan.models.Ingrediente
import com.christian.nutriplan.models.Ingredientes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction

class IngredienteService(private val database: Database) {
    fun create(ingrediente: Ingrediente): Int = transaction(database) {
        Ingredientes.insert {
            it[nombre] = ingrediente.nombre
            it[categoriaId] = ingrediente.categoriaId
            it[calorias] = ingrediente.calorias?.toBigDecimal()
        }[Ingredientes.ingredienteId]
    }

    fun read(id: Int): Ingrediente? = transaction(database) {
        Ingredientes.select(Ingredientes.ingredienteId eq id)
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }.singleOrNull()
    }

    fun update(id: Int, ingrediente: Ingrediente) = transaction(database) {
        Ingredientes.update({ Ingredientes.ingredienteId eq id }) {
            it[nombre] = ingrediente.nombre
            it[categoriaId] = ingrediente.categoriaId
            it[calorias] = ingrediente.calorias?.toBigDecimal()
        }
    }

    fun delete(id: Int) = transaction(database) {
        Ingredientes.deleteWhere { Ingredientes.ingredienteId eq id }
    }

    fun getAll(): List<Ingrediente> = transaction(database) {
        Ingredientes.selectAll()
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }
    }

    fun searchByName(query: String): List<Ingrediente> = transaction(database) {
        Ingredientes.select(Ingredientes.nombre like "%$query%")
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }
    }

    fun getByCategoria(categoriaId: Int): List<Ingrediente> = transaction(database) {
        Ingredientes.select(Ingredientes.categoriaId eq categoriaId)
            .map {
                Ingrediente(
                    ingredienteId = it[Ingredientes.ingredienteId],
                    nombre = it[Ingredientes.nombre],
                    categoriaId = it[Ingredientes.categoriaId],
                    calorias = it[Ingredientes.calorias]?.toDouble()
                )
            }
    }
}
