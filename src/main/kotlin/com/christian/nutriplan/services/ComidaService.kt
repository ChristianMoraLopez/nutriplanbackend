package com.christian.nutriplan.services

import com.christian.nutriplan.models.Comida
import com.christian.nutriplan.models.Comidas
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ComidaService(private val database: Database) {
    fun create(comida: Comida): Int = transaction(database) {
        Comidas.insert {
            it[nombre] = comida.nombre
        }[Comidas.comidaId]
    }

    fun read(id: Int): Comida? = transaction(database) {
        Comidas.select(Comidas.comidaId eq id)
            .map {
                Comida(
                    comidaId = it[Comidas.comidaId],
                    nombre = it[Comidas.nombre]
                )
            }.singleOrNull()
    }

    fun update(id: Int, comida: Comida) = transaction(database) {
        Comidas.update({ Comidas.comidaId eq id }) {
            it[nombre] = comida.nombre
        }
    }

    fun delete(id: Int) = transaction(database) {
        Comidas.deleteWhere { Comidas.comidaId eq id }
    }

    fun getAll(): List<Comida> = transaction(database) {
        Comidas.selectAll()
            .map {
                Comida(
                    comidaId = it[Comidas.comidaId],
                    nombre = it[Comidas.nombre]
                )
            }
    }
}
