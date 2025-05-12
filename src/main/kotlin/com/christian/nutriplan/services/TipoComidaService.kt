package com.christian.nutriplan.services

import com.christian.nutriplan.models.TipoComida
import com.christian.nutriplan.models.TiposComida
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class TipoComidaService(private val database: Database) {
    fun create(tipoComida: TipoComida): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val tipoComidaId = TiposComida.insertAndGetId {
                it[nombre] = tipoComida.nombre
            }.value
            println("Tipo de comida creado con ID: $tipoComidaId")
            return@transaction tipoComidaId
        } catch (e: Exception) {
            println("Error al crear tipo de comida: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): TipoComida? = transaction(database) {
        addLogger(StdOutSqlLogger)
        TiposComida.selectAll().where { TiposComida.id eq id }.map {
            TipoComida(
                tipoComidaId = it[TiposComida.id].value,
                nombre = it[TiposComida.nombre]
            )
        }.singleOrNull().also {
            println("Tipo de comida encontrado para id $id: ${it != null}")
        }
    }

    fun update(id: Int, tipoComida: TipoComida) = transaction(database) {
        addLogger(StdOutSqlLogger)
        TiposComida.update({ TiposComida.id eq id }) {
            it[nombre] = tipoComida.nombre
        }.also {
            println("Tipo de comida actualizado para id $id: $it filas afectadas")
        }
    }

    fun delete(id: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        TiposComida.deleteWhere { TiposComida.id eq id }.also {
            println("Tipo de comida eliminado para id $id: $it filas afectadas")
        }
    }

    fun getAll(): List<TipoComida> = transaction(database) {
        addLogger(StdOutSqlLogger)
        TiposComida.selectAll().map {
            TipoComida(
                tipoComidaId = it[TiposComida.id].value,
                nombre = it[TiposComida.nombre]
            )
        }.also {
            println("Total tipos de comida obtenidos: ${it.size}")
        }
    }

    fun getTiposComidaPaginated(page: Int, pageSize: Int): List<TipoComida> = transaction(database) {
        addLogger(StdOutSqlLogger)
        TiposComida.selectAll()
            .limit(pageSize).offset((page - 1) * pageSize.toLong())
            .map {
                TipoComida(
                    tipoComidaId = it[TiposComida.id].value,
                    nombre = it[TiposComida.nombre]
                )
            }.also {
                println("Tipos de comida obtenidos para página $page (tamaño $pageSize): ${it.size}")
            }
    }
}