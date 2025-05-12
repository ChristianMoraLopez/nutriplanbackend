package com.christian.nutriplan.services

import com.christian.nutriplan.models.MetodoPreparacion
import com.christian.nutriplan.models.MetodosPreparacion
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


class MetodoPreparacionService(private val database: Database) {
    fun create(metodo: MetodoPreparacion): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val metodoId = MetodosPreparacion.insertAndGetId {
                it[nombre] = metodo.nombre
                it[descripcion] = metodo.descripcion
            }.value
            println("Método de preparación creado con ID: $metodoId")
            return@transaction metodoId
        } catch (e: Exception) {
            println("Error al crear método de preparación: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): MetodoPreparacion? = transaction(database) {
        addLogger(StdOutSqlLogger)
        MetodosPreparacion.selectAll().where { MetodosPreparacion.id eq id }.map {
            MetodoPreparacion(
                metodoId = it[MetodosPreparacion.id].value,
                nombre = it[MetodosPreparacion.nombre],
                descripcion = it[MetodosPreparacion.descripcion]
            )
        }.singleOrNull().also {
            println("Método encontrado para id $id: ${it != null}")
        }
    }

    fun update(id: Int, metodo: MetodoPreparacion) = transaction(database) {
        addLogger(StdOutSqlLogger)
        MetodosPreparacion.update({ MetodosPreparacion.id eq id }) {
            it[nombre] = metodo.nombre
            it[descripcion] = metodo.descripcion
        }.also {
            println("Método actualizado para id $id: $it filas afectadas")
        }
    }

    fun delete(id: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        MetodosPreparacion.deleteWhere { MetodosPreparacion.id eq id }.also {
            println("Método eliminado para id $id: $it filas afectadas")
        }
    }

    fun getAll(): List<MetodoPreparacion> = transaction(database) {
        addLogger(StdOutSqlLogger)
        MetodosPreparacion.selectAll().map {
            MetodoPreparacion(
                metodoId = it[MetodosPreparacion.id].value,
                nombre = it[MetodosPreparacion.nombre],
                descripcion = it[MetodosPreparacion.descripcion]
            )
        }.also {
            println("Total métodos de preparación obtenidos: ${it.size}")
        }
    }

    fun getMetodosPaginated(page: Int, pageSize: Int): List<MetodoPreparacion> = transaction(database) {
        addLogger(StdOutSqlLogger)
        MetodosPreparacion.selectAll()
            .limit(pageSize).offset((page - 1) * pageSize.toLong())
            .map {
                MetodoPreparacion(
                    metodoId = it[MetodosPreparacion.id].value,
                    nombre = it[MetodosPreparacion.nombre],
                    descripcion = it[MetodosPreparacion.descripcion]
                )
            }.also {
                println("Métodos obtenidos para página $page (tamaño $pageSize): ${it.size}")
            }
    }
}