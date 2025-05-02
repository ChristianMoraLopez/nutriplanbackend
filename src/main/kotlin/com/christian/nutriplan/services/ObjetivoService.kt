package com.christian.nutriplan.services

import com.christian.nutriplan.models.Objetivo
import com.christian.nutriplan.models.Objetivos
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ObjetivoService(private val database: Database) {
    fun create(objetivo: Objetivo): Int = transaction(database) {
            Objetivos.insert {
            it[nombre] = objetivo.nombre
            it[tieneTiempo] = objetivo.tieneTiempo
        }[Objetivos.objetivoId]
    }

    fun read(id: Int): Objetivo? = transaction(database) {
        Objetivos.select(Objetivos.objetivoId eq id)
            .map {
                Objetivo(
                    objetivoId = it[Objetivos.objetivoId],
                    nombre = it[Objetivos.nombre],
                    tieneTiempo = it[Objetivos.tieneTiempo]
                )
            }.singleOrNull()
    }

    fun update(id: Int, objetivo: Objetivo) = transaction(database) {
        Objetivos.update({ Objetivos.objetivoId eq id }) {
            it[nombre] = objetivo.nombre
            it[tieneTiempo] = objetivo.tieneTiempo
        }
    }

    fun delete(id: Int) = transaction(database) {
        Objetivos.deleteWhere { Objetivos.objetivoId eq id }
    }

    fun getAll(): List<Objetivo> = transaction(database) {
        Objetivos.selectAll()
            .map {
                Objetivo(
                    objetivoId = it[Objetivos.objetivoId],
                    nombre = it[Objetivos.nombre],
                    tieneTiempo = it[Objetivos.tieneTiempo]
                )
            }
    }
}
