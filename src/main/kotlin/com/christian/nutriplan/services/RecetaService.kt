package com.christian.nutriplan.services

import com.christian.nutriplan.models.Receta
import com.christian.nutriplan.models.Recetas
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class RecetaService(private val database: Database) {
    fun create(receta: Receta): Int = transaction(database) {
        addLogger(StdOutSqlLogger)
        try {
            val recetaId = Recetas.insertAndGetId {
                it[nombre] = receta.nombre
                it[tipoComidaId] = receta.tipoComidaId
                it[fit] = receta.fit
                it[instrucciones] = receta.instrucciones
                it[tiempoPreparacion] = receta.tiempoPreparacion
                it[disponibleBogota] = receta.disponibleBogota
                it[metodoId] = receta.metodoId
            }.value

            println("Receta creada con ID: $recetaId")
            return@transaction recetaId
        } catch (e: Exception) {
            println("Error al crear receta: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): Receta? = transaction(database) {
        addLogger(StdOutSqlLogger)
        Recetas.selectAll().where { Recetas.id eq id }.map {
            Receta(
                recetaId = it[Recetas.id].value,
                nombre = it[Recetas.nombre],
                tipoComidaId = it[Recetas.tipoComidaId],
                fit = it[Recetas.fit],
                instrucciones = it[Recetas.instrucciones],
                tiempoPreparacion = it[Recetas.tiempoPreparacion],
                disponibleBogota = it[Recetas.disponibleBogota],
                metodoId = it[Recetas.metodoId]
            )
        }.singleOrNull().also {
            println("Receta encontrada para id $id: ${it != null}")
        }
    }

    fun update(id: Int, receta: Receta) = transaction(database) {
        addLogger(StdOutSqlLogger)
        Recetas.update({ Recetas.id eq id }) {
            it[nombre] = receta.nombre
            it[tipoComidaId] = receta.tipoComidaId
            it[fit] = receta.fit
            it[instrucciones] = receta.instrucciones
            it[tiempoPreparacion] = receta.tiempoPreparacion
            it[disponibleBogota] = receta.disponibleBogota
            it[metodoId] = receta.metodoId
        }.also {
            println("Receta actualizada para id $id: $it filas afectadas")
        }
    }

    fun delete(id: Int) = transaction(database) {
        addLogger(StdOutSqlLogger)
        Recetas.deleteWhere { Recetas.id eq id }.also {
            println("Receta eliminada para id $id: $it filas afectadas")
        }
    }

    fun getAll(): List<Receta> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Recetas.selectAll().map {
            Receta(
                recetaId = it[Recetas.id].value,
                nombre = it[Recetas.nombre],
                tipoComidaId = it[Recetas.tipoComidaId],
                fit = it[Recetas.fit],
                instrucciones = it[Recetas.instrucciones],
                tiempoPreparacion = it[Recetas.tiempoPreparacion],
                disponibleBogota = it[Recetas.disponibleBogota],
                metodoId = it[Recetas.metodoId]
            )
        }.also {
            println("Total recetas obtenidas: ${it.size}")
        }
    }

    fun getRecetasPaginated(page: Int, pageSize: Int): List<Receta> = transaction(database) {
        addLogger(StdOutSqlLogger)
        Recetas.selectAll()
            .limit(pageSize).offset((page - 1) * pageSize.toLong())
            .map {
                Receta(
                    recetaId = it[Recetas.id].value,
                    nombre = it[Recetas.nombre],
                    tipoComidaId = it[Recetas.tipoComidaId],
                    fit = it[Recetas.fit],
                    instrucciones = it[Recetas.instrucciones],
                    tiempoPreparacion = it[Recetas.tiempoPreparacion],
                    disponibleBogota = it[Recetas.disponibleBogota],
                    metodoId = it[Recetas.metodoId]
                )
            }.also {
                println("Recetas obtenidas para página $page (tamaño $pageSize): ${it.size}")
            }
    }
}