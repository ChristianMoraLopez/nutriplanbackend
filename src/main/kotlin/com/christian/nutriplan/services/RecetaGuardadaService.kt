package com.christian.nutriplan.services

import com.christian.nutriplan.models.RecetaGuardada
import com.christian.nutriplan.models.Recetas
import com.christian.nutriplan.models.RecetasGuardadas
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class RecetaGuardadaService(private val database: Database) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun create(recetaGuardada: RecetaGuardada): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val guardadoId = RecetasGuardadas.insertAndGetId {
                it[usuarioId] = recetaGuardada.usuarioId
                it[recetaId] = recetaGuardada.recetaId
                it[fechaGuardado] = LocalDateTime.now()
                it[comentarioPersonal] = recetaGuardada.comentarioPersonal
            }.value
            println("Receta guardada creada con ID: $guardadoId")
            return@transaction guardadoId
        } catch (e: Exception) {
            println("Error al crear receta guardada: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): RecetaGuardada? = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetasGuardadas innerJoin Recetas)
            .selectAll()
            .where { RecetasGuardadas.id eq id }
            .map {
                RecetaGuardada(
                    guardadoId = it[RecetasGuardadas.id].value,
                    usuarioId = it[RecetasGuardadas.usuarioId],
                    recetaId = it[RecetasGuardadas.recetaId],
                    fechaGuardado = it[RecetasGuardadas.fechaGuardado].format(formatter),
                    comentarioPersonal = it[RecetasGuardadas.comentarioPersonal],
                    nombreReceta = it[Recetas.nombre]
                )
            }.singleOrNull().also {
                println("Receta guardada encontrada para id $id: ${it != null}")
            }
    }

    fun update(id: Int, recetaGuardada: RecetaGuardada) = transaction(database) {
        addLogger(StdOutSqlLogger)
        RecetasGuardadas.update({ RecetasGuardadas.id eq id }) {
            it[usuarioId] = recetaGuardada.usuarioId
            it[recetaId] = recetaGuardada.recetaId
            it[comentarioPersonal] = recetaGuardada.comentarioPersonal
        }.also {
            println("Receta guardada actualizada para id $id: $it filas afectadas")
        }
    }

    fun delete(id: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        RecetasGuardadas.deleteWhere { RecetasGuardadas.id eq id }.also {
            println("Receta guardada eliminada para id $id: $it filas afectadas")
        }
    }

    fun getByUsuario(usuarioId: Int): List<RecetaGuardada> = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetasGuardadas innerJoin Recetas)
            .selectAll()
            .where { RecetasGuardadas.usuarioId eq usuarioId }
            .map {
                RecetaGuardada(
                    guardadoId = it[RecetasGuardadas.id].value,
                    usuarioId = it[RecetasGuardadas.usuarioId],
                    recetaId = it[RecetasGuardadas.recetaId],
                    fechaGuardado = it[RecetasGuardadas.fechaGuardado].format(formatter),
                    comentarioPersonal = it[RecetasGuardadas.comentarioPersonal],
                    nombreReceta = it[Recetas.nombre]
                )
            }.also {
                println("Recetas guardadas encontradas para usuarioId $usuarioId: ${it.size}")
            }
    }

    fun getAll(): List<RecetaGuardada> = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetasGuardadas innerJoin Recetas)
            .selectAll()
            .map {
                RecetaGuardada(
                    guardadoId = it[RecetasGuardadas.id].value,
                    usuarioId = it[RecetasGuardadas.usuarioId],
                    recetaId = it[RecetasGuardadas.recetaId],
                    fechaGuardado = it[RecetasGuardadas.fechaGuardado].format(formatter),
                    comentarioPersonal = it[RecetasGuardadas.comentarioPersonal],
                    nombreReceta = it[Recetas.nombre]
                )
            }.also {
                println("Total recetas guardadas obtenidas: ${it.size}")
            }
    }

    fun getRecetasGuardadasPaginated(page: Int, pageSize: Int): List<RecetaGuardada> = transaction(database) {
        addLogger(StdOutSqlLogger)
        (RecetasGuardadas innerJoin Recetas)
            .selectAll()
            .limit(pageSize)
            .offset((page - 1) * pageSize.toLong())
            .map {
                RecetaGuardada(
                    guardadoId = it[RecetasGuardadas.id].value,
                    usuarioId = it[RecetasGuardadas.usuarioId],
                    recetaId = it[RecetasGuardadas.recetaId],
                    fechaGuardado = it[RecetasGuardadas.fechaGuardado].format(formatter),
                    comentarioPersonal = it[RecetasGuardadas.comentarioPersonal],
                    nombreReceta = it[Recetas.nombre]
                )
            }.also {
                println("Recetas guardadas obtenidas para página $page (tamaño $pageSize): ${it.size}")
            }
    }
}