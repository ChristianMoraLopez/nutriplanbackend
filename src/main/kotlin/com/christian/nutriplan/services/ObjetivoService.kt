package com.christian.nutriplan.services

import com.christian.nutriplan.models.Objetivo
import com.christian.nutriplan.models.Objetivos
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ObjetivoService(private val database: Database) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun create(objetivo: Objetivo): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            if (objetivo.nombre.isBlank()) {
                throw IllegalArgumentException("Nombre no puede estar vacío")
            }
            val parsedFechaCreacion = objetivo.fechaCreacion?.let {
                try {
                    LocalDateTime.parse(it, dateFormatter)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Formato de fechaCreacion inválido: ${e.message}")
                }
            } ?: LocalDateTime.now()

            val objetivoId = Objetivos.insertAndGetId {
                it[nombre] = objetivo.nombre
                it[tieneTiempo] = objetivo.tieneTiempo
                it[fechaCreacion] = parsedFechaCreacion
                it[usuarioId] = objetivo.usuarioId
            }.value

            println("Objetivo creado con ID: $objetivoId")
            objetivoId
        } catch (e: Exception) {
            println("Error al crear objetivo: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): Objetivo? = transaction(database) {
        addLogger(StdOutSqlLogger)
        Objetivos.selectAll().where { Objetivos.id eq id }
            .singleOrNull()?.let {
                Objetivo(
                    objetivoId = it[Objetivos.id].value,
                    nombre = it[Objetivos.nombre],
                    tieneTiempo = it[Objetivos.tieneTiempo],
                    fechaCreacion = it[Objetivos.fechaCreacion].format(dateFormatter),
                    usuarioId = it[Objetivos.usuarioId]
                )
            }.also {
                println("Objetivo encontrado para id $id: ${it != null}")
            }
    }

    fun update(id: Int, objetivo: Objetivo) = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            if (objetivo.nombre.isBlank()) {
                throw IllegalArgumentException("Nombre no puede estar vacío")
            }
            val parsedFechaCreacion = objetivo.fechaCreacion?.let {
                try {
                    LocalDateTime.parse(it, dateFormatter)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Formato de fechaCreacion inválido: ${e.message}")
                }
            } ?: LocalDateTime.now()

            Objetivos.update({ Objetivos.id eq id }) {
                it[nombre] = objetivo.nombre
                it[tieneTiempo] = objetivo.tieneTiempo
                it[fechaCreacion] = parsedFechaCreacion
                it[usuarioId] = objetivo.usuarioId
            }
        } catch (e: Exception) {
            println("Error al actualizar objetivo: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun delete(id: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            Objetivos.deleteWhere { Objetivos.id eq id }
        } catch (e: Exception) {
            println("Error al eliminar objetivo: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun getAll(usuarioId: Int? = null): List<Objetivo> = transaction(database) {
        addLogger(StdOutSqlLogger)
        val query = when (usuarioId) {
            null -> Objetivos.selectAll()
            else -> Objetivos.selectAll().where { Objetivos.usuarioId eq usuarioId }
        }
        query.orderBy(Objetivos.fechaCreacion to SortOrder.DESC)
            .map {
                Objetivo(
                    objetivoId = it[Objetivos.id].value,
                    nombre = it[Objetivos.nombre],
                    tieneTiempo = it[Objetivos.tieneTiempo],
                    fechaCreacion = it[Objetivos.fechaCreacion].format(dateFormatter),
                    usuarioId = it[Objetivos.usuarioId]
                )
            }
    }
}