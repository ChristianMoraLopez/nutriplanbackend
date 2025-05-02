package com.christian.nutriplan.services

import com.christian.nutriplan.models.Ingredientes
import com.christian.nutriplan.models.SeleccionIngrediente
import com.christian.nutriplan.models.SeleccionIngredientes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class SeleccionIngredienteService(private val database: Database) {
    fun create(seleccion: SeleccionIngrediente): Int = transaction(database) {
        SeleccionIngredientes.insert {
            it[menuId] = seleccion.menuId
            it[ingredienteId] = seleccion.ingredienteId
            it[cantidad] = seleccion.cantidad?.toBigDecimal()
        }[SeleccionIngredientes.seleccionId]
    }

    fun read(id: Int): SeleccionIngrediente? = transaction(database) {
        SeleccionIngredientes.select(SeleccionIngredientes.seleccionId eq id)
            .map {
                SeleccionIngrediente(
                    seleccionId = it[SeleccionIngredientes.seleccionId],
                    menuId = it[SeleccionIngredientes.menuId],
                    ingredienteId = it[SeleccionIngredientes.ingredienteId],
                    cantidad = it[SeleccionIngredientes.cantidad]?.toDouble()
                )
            }.singleOrNull()
    }

    fun update(id: Int, seleccion: SeleccionIngrediente) = transaction(database) {
        SeleccionIngredientes.update({ SeleccionIngredientes.seleccionId eq id }) {
            it[menuId] = seleccion.menuId
            it[ingredienteId] = seleccion.ingredienteId
            it[cantidad] = seleccion.cantidad?.toBigDecimal()
        }
    }

    fun delete(id: Int) = transaction(database) {
        SeleccionIngredientes.deleteWhere { SeleccionIngredientes.seleccionId eq id }
    }

    fun getByMenu(menuId: Int): List<SeleccionIngrediente> = transaction(database) {
        SeleccionIngredientes.select(SeleccionIngredientes.menuId eq menuId)
            .map {
                SeleccionIngrediente(
                    seleccionId = it[SeleccionIngredientes.seleccionId],
                    menuId = it[SeleccionIngredientes.menuId],
                    ingredienteId = it[SeleccionIngredientes.ingredienteId],
                    cantidad = it[SeleccionIngredientes.cantidad]?.toDouble()
                )
            }
    }

    fun getSeleccionesConIngredientes(menuId: Int): List<Map<String, Any?>> = transaction(database) {
        (SeleccionIngredientes innerJoin Ingredientes)
            .select(SeleccionIngredientes.menuId eq menuId)
            .map {
                mapOf(
                    "seleccionId" to it[SeleccionIngredientes.seleccionId],
                    "menuId" to it[SeleccionIngredientes.menuId],
                    "ingredienteId" to it[SeleccionIngredientes.ingredienteId],
                    "cantidad" to it[SeleccionIngredientes.cantidad]?.toDouble(),
                    "nombreIngrediente" to it[Ingredientes.nombre],
                    "calorias" to it[Ingredientes.calorias]?.toDouble()
                )
            }
    }
}
