package com.christian.nutriplan.services

import com.christian.nutriplan.models.CategoriaIngrediente
import com.christian.nutriplan.models.CategoriasIngredientes
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CategoriaIngredienteService(private val database: Database) {
    fun create(categoria: CategoriaIngrediente): Int = transaction(database) {
        CategoriasIngredientes.insert {
            it[nombre] = categoria.nombre
        }[CategoriasIngredientes.categoriaId]
    }

    fun read(id: Int): CategoriaIngrediente? = transaction(database) {
        CategoriasIngredientes.select(CategoriasIngredientes.categoriaId eq id)
            .map {
                CategoriaIngrediente(
                    categoriaId = it[CategoriasIngredientes.categoriaId],
                    nombre = it[CategoriasIngredientes.nombre]
                )
            }.singleOrNull()
    }

    fun update(id: Int, categoria: CategoriaIngrediente) = transaction(database) {
        CategoriasIngredientes.update({ CategoriasIngredientes.categoriaId eq id }) {
            it[nombre] = categoria.nombre
        }
    }

    fun delete(id: Int) = transaction(database) {
        CategoriasIngredientes.deleteWhere { CategoriasIngredientes.categoriaId eq id }
    }

    fun getAll(): List<CategoriaIngrediente> = transaction(database) {
        CategoriasIngredientes.selectAll()
            .map {
                CategoriaIngrediente(
                    categoriaId = it[CategoriasIngredientes.categoriaId],
                    nombre = it[CategoriasIngredientes.nombre]
                )
            }
    }
}
