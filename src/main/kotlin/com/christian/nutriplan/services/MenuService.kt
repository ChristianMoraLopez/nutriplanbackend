package com.christian.nutriplan.services

import com.christian.nutriplan.models.Menu
import com.christian.nutriplan.models.Menus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class MenuService(private val database: Database) {
    fun create(menu: Menu): Int = transaction(database) {
        Menus.insert {
            it[usuarioId] = menu.usuarioId
            it[objetivoId] = menu.objetivoId
            it[comidaId] = menu.comidaId
            it[metodoId] = menu.metodoId
        }[Menus.menuId]
    }

    fun read(id: Int): Menu? = transaction(database) {
        Menus.select(Menus.menuId eq id)
            .map {
                Menu(
                    menuId = it[Menus.menuId],
                    usuarioId = it[Menus.usuarioId].value,
                    objetivoId = it[Menus.objetivoId],
                    comidaId = it[Menus.comidaId],
                    fechaCreacion = it[Menus.fechaCreacion].toString(),
                    metodoId = it[Menus.metodoId]
                )
            }.singleOrNull()
    }

    fun update(id: Int, menu: Menu) = transaction(database) {
        Menus.update({ Menus.menuId eq id }) {
            it[usuarioId] = menu.usuarioId
            it[objetivoId] = menu.objetivoId
            it[comidaId] = menu.comidaId
            it[metodoId] = menu.metodoId
        }
    }

    fun delete(id: Int) = transaction(database) {
        Menus.deleteWhere { Menus.menuId eq id }
    }

    fun getAll(): List<Menu> = transaction(database) {
        Menus.selectAll()
            .map {
                Menu(
                    menuId = it[Menus.menuId],
                    usuarioId = it[Menus.usuarioId].value,
                    objetivoId = it[Menus.objetivoId],
                    comidaId = it[Menus.comidaId],
                    fechaCreacion = it[Menus.fechaCreacion].toString(),
                    metodoId = it[Menus.metodoId]
                )
            }
    }

    fun getByUsuario(usuarioId: Int): List<Menu> = transaction(database) {
        Menus.select(Menus.usuarioId eq usuarioId)
            .map {
                Menu(
                    menuId = it[Menus.menuId],
                    usuarioId = it[Menus.usuarioId].value,
                    objetivoId = it[Menus.objetivoId],
                    comidaId = it[Menus.comidaId],
                    fechaCreacion = it[Menus.fechaCreacion].toString(),
                    metodoId = it[Menus.metodoId]
                )
            }
    }

    fun getByObjetivo(objetivoId: Int): List<Menu> = transaction(database) {
        Menus.select(Menus.objetivoId eq objetivoId)
            .map {
                Menu(
                    menuId = it[Menus.menuId],
                    usuarioId = it[Menus.usuarioId].value,
                    objetivoId = it[Menus.objetivoId],
                    comidaId = it[Menus.comidaId],
                    fechaCreacion = it[Menus.fechaCreacion].toString(),
                    metodoId = it[Menus.metodoId]
                )
            }
    }

    fun getByComida(comidaId: Int): List<Menu> = transaction(database) {
        Menus.select(Menus.comidaId eq comidaId)
            .map {
                Menu(
                    menuId = it[Menus.menuId],
                    usuarioId = it[Menus.usuarioId].value,
                    objetivoId = it[Menus.objetivoId],
                    comidaId = it[Menus.comidaId],
                    fechaCreacion = it[Menus.fechaCreacion].toString(),
                    metodoId = it[Menus.metodoId]
                )
            }
    }
}
