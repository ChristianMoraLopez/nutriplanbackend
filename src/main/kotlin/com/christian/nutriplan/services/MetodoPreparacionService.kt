package com.christian.nutriplan.services

import com.christian.nutriplan.models.MetodoPreparacion
import com.christian.nutriplan.models.MetodosPreparacion
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class MetodoPreparacionService(private val database: Database) {
    fun create(metodo: MetodoPreparacion): Int = transaction(database) {
        MetodosPreparacion.insert {
            it[nombre] = metodo.nombre
            it[descripcion] = metodo.descripcion
        }[MetodosPreparacion.metodoId]
    }

    fun read(id: Int): MetodoPreparacion? = transaction(database) {
        MetodosPreparacion.select(MetodosPreparacion.metodoId eq id)
            .map {
                MetodoPreparacion(
                    metodoId = it[MetodosPreparacion.metodoId],
                    nombre = it[MetodosPreparacion.nombre],
                    descripcion = it[MetodosPreparacion.descripcion]
                )
            }.singleOrNull()
    }

    fun update(id: Int, metodo: MetodoPreparacion) = transaction(database) {
        MetodosPreparacion.update({ MetodosPreparacion.metodoId eq id }) {
            it[nombre] = metodo.nombre
            it[descripcion] = metodo.descripcion
        }
    }

    fun delete(id: Int) = transaction(database) {
        MetodosPreparacion.deleteWhere { MetodosPreparacion.metodoId eq id }
    }

    fun getAll(): List<MetodoPreparacion> = transaction(database) {
        MetodosPreparacion.selectAll()
            .map {
                MetodoPreparacion(
                    metodoId = it[MetodosPreparacion.metodoId],
                    nombre = it[MetodosPreparacion.nombre],
                    descripcion = it[MetodosPreparacion.descripcion]
                )
            }
    }
}
