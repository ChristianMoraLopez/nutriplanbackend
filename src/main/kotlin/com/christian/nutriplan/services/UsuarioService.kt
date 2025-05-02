package com.christian.nutriplan.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.christian.nutriplan.models.Usuario
import com.christian.nutriplan.models.Usuarios
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.slice


class UsuarioService(private val database: Database) {
    fun create(usuario: Usuario): Int = transaction(database) {
        val bcrypt = BCrypt.withDefaults()
        val hashedPassword = bcrypt.hashToString(12, usuario.contrasena.toCharArray())
        addLogger(StdOutSqlLogger)
        try {
            // Aquí está el cambio clave - usamos Usuarios.id que es creado por IntIdTable
            val usuarioId = Usuarios.insertAndGetId {
                it[nombre] = usuario.nombre
                it[email] = usuario.email
                it[contrasena] = hashedPassword
                it[aceptaTerminos] = usuario.aceptaTerminos
                it[rol] = usuario.rol
                it[fechaRegistro] = LocalDateTime.now()
            }.value

            println("Usuario creado con ID: $usuarioId")
            return@transaction usuarioId
        } catch (e: Exception) {
            println("Error al crear usuario: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun read(id: Int): Usuario? = transaction(database) {
        addLogger(StdOutSqlLogger)
        Usuarios.selectAll().where { Usuarios.id eq id }.map {
            Usuario(
                usuarioId = it[Usuarios.id].value,
                nombre = it[Usuarios.nombre],
                email = it[Usuarios.email],
                contrasena = "", // Never return password hash
                aceptaTerminos = it[Usuarios.aceptaTerminos],
                rol = it[Usuarios.rol],
                fechaRegistro = it[Usuarios.fechaRegistro].toString()
            )
        }.singleOrNull().also {
            println("User found for id $id: ${it != null}")
        }
    }

    fun update(id: Int, usuario: Usuario) = transaction(database) {
        Usuarios.update({ Usuarios.id eq id }) {
            it[nombre] = usuario.nombre
            it[email] = usuario.email
            if (usuario.contrasena.isNotEmpty()) {
                it[contrasena] = usuario.contrasena
            }
            it[aceptaTerminos] = usuario.aceptaTerminos
            it[rol] = usuario.rol
        }
    }

    fun delete(id: Int) = transaction(database) {
        Usuarios.deleteWhere { Usuarios.id eq id }
    }

    fun login(email: String, password: String): Usuario? = transaction(database) {
        Usuarios.select((Usuarios.email eq email) and (Usuarios.contrasena eq password))
            .map {
                Usuario(
                    usuarioId = it[Usuarios.id].value,  // Asegurándonos de mapear correctamente
                    nombre = it[Usuarios.nombre],
                    email = it[Usuarios.email],
                    contrasena = "",  // No devolvemos la contraseña por seguridad
                    aceptaTerminos = it[Usuarios.aceptaTerminos],
                    rol = it[Usuarios.rol],
                    fechaRegistro = it[Usuarios.fechaRegistro].toString()
                )
            }.singleOrNull()
    }

    fun getAllUsers(): List<Usuario> = transaction(database) {
        Usuarios.selectAll()
            .map {
                Usuario(
                    usuarioId = it[Usuarios.id].value,
                    nombre = it[Usuarios.nombre],
                    email = it[Usuarios.email],
                    contrasena = "",  // No devolvemos la contraseña por seguridad
                    aceptaTerminos = it[Usuarios.aceptaTerminos],
                    rol = it[Usuarios.rol],
                    fechaRegistro = it[Usuarios.fechaRegistro].toString()
                )
            }
    }

    fun getUsersPaginated(page: Int, pageSize: Int): List<Usuario> = transaction(database) {
        Usuarios.selectAll()
            .limit(pageSize).offset(start = (page - 1) * pageSize.toLong())
            .map {
                Usuario(
                    usuarioId = it[Usuarios.id].value,
                    nombre = it[Usuarios.nombre],
                    email = it[Usuarios.email],
                    contrasena = "",  // No devolvemos la contraseña por seguridad
                    aceptaTerminos = it[Usuarios.aceptaTerminos],
                    rol = it[Usuarios.rol],
                    fechaRegistro = it[Usuarios.fechaRegistro].toString()
                )
            }
    }
}