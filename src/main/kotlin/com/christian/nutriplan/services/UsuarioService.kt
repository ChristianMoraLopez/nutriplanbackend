package com.christian.nutriplan.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.christian.nutriplan.models.Usuario
import com.christian.nutriplan.models.Usuarios
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class UsuarioService(private val database: Database) {
    fun create(usuario: Usuario): Int = transaction(database) {
        val bcrypt = BCrypt.withDefaults()
        val hashedPassword = if (usuario.contrasena.isNotEmpty()) {
            bcrypt.hashToString(12, usuario.contrasena.toCharArray())
        } else {
            "" // No password for Google Sign-In users
        }
        addLogger(StdOutSqlLogger)
        try {
            val usuarioId = Usuarios.insertAndGetId {
                it[nombre] = usuario.nombre
                it[email] = usuario.email
                it[contrasena] = hashedPassword
                it[aceptaTerminos] = usuario.aceptaTerminos
                it[rol] = usuario.rol
                it[fechaRegistro] = LocalDateTime.now()
                it[ciudad] = usuario.ciudad
                it[localidad] = usuario.localidad
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
                contrasena = "",
                aceptaTerminos = it[Usuarios.aceptaTerminos],
                rol = it[Usuarios.rol],
                fechaRegistro = it[Usuarios.fechaRegistro].toString(),
                ciudad = it[Usuarios.ciudad],
                localidad = it[Usuarios.localidad]
            )
        }.singleOrNull().also {
            println("User found for id $id: ${it != null}")
        }
    }

    fun findByEmail(email: String): Usuario? = transaction(database) {
        addLogger(StdOutSqlLogger)
        Usuarios.selectAll().where { Usuarios.email eq email }.map {
            Usuario(
                usuarioId = it[Usuarios.id].value,
                nombre = it[Usuarios.nombre],
                email = it[Usuarios.email],
                contrasena = "",
                aceptaTerminos = it[Usuarios.aceptaTerminos],
                rol = it[Usuarios.rol],
                fechaRegistro = it[Usuarios.fechaRegistro].toString(),
                ciudad = it[Usuarios.ciudad],
                localidad = it[Usuarios.localidad]
            )
        }.singleOrNull()
    }

    fun update(id: Int, usuario: Usuario) = transaction(database) {
        val bcrypt = BCrypt.withDefaults()
        Usuarios.update({ Usuarios.id eq id }) {
            it[nombre] = usuario.nombre
            it[email] = usuario.email
            if (usuario.contrasena.isNotEmpty()) {
                val hashedPassword = bcrypt.hashToString(12, usuario.contrasena.toCharArray())
                it[contrasena] = hashedPassword
            }
            it[aceptaTerminos] = usuario.aceptaTerminos
            it[rol] = usuario.rol
            it[ciudad] = usuario.ciudad
            it[localidad] = usuario.localidad
        }.also { rowsAffected ->
            println("Updated user $id: $rowsAffected rows affected")
        }
    }

    fun delete(id: Int) = transaction(database) {
        Usuarios.deleteWhere { Usuarios.id eq id }
    }

    fun login(email: String, password: String): Usuario? = transaction(database) {
        addLogger(StdOutSqlLogger)
        val user = Usuarios.selectAll().where { Usuarios.email eq email }
            .singleOrNull()
            ?.let {
                Usuario(
                    usuarioId = it[Usuarios.id].value,
                    nombre = it[Usuarios.nombre],
                    email = it[Usuarios.email],
                    contrasena = it[Usuarios.contrasena],
                    aceptaTerminos = it[Usuarios.aceptaTerminos],
                    rol = it[Usuarios.rol],
                    fechaRegistro = it[Usuarios.fechaRegistro].toString(),
                    ciudad = it[Usuarios.ciudad],
                    localidad = it[Usuarios.localidad]
                )
            }

        if (user != null && user.contrasena.isNotEmpty() && BCrypt.verifyer().verify(password.toCharArray(), user.contrasena).verified) {
            return@transaction user.copy(contrasena = "")
        }
        println("Login failed for email: $email")
        return@transaction null
    }

    fun getAllUsers(): List<Usuario> = transaction(database) {
        Usuarios.selectAll()
            .map {
                Usuario(
                    usuarioId = it[Usuarios.id].value,
                    nombre = it[Usuarios.nombre],
                    email = it[Usuarios.email],
                    contrasena = "",
                    aceptaTerminos = it[Usuarios.aceptaTerminos],
                    rol = it[Usuarios.rol],
                    fechaRegistro = it[Usuarios.fechaRegistro].toString(),
                    ciudad = it[Usuarios.ciudad],
                    localidad = it[Usuarios.localidad]
                )
            }
    }

    fun getUsersPaginated(page: Int, pageSize: Int): List<Usuario> = transaction(database) {
        Usuarios.selectAll()
            .limit(pageSize).offset((page - 1) * pageSize.toLong())
            .map {
                Usuario(
                    usuarioId = it[Usuarios.id].value,
                    nombre = it[Usuarios.nombre],
                    email = it[Usuarios.email],
                    contrasena = "",
                    aceptaTerminos = it[Usuarios.aceptaTerminos],
                    rol = it[Usuarios.rol],
                    fechaRegistro = it[Usuarios.fechaRegistro].toString(),
                    ciudad = it[Usuarios.ciudad],
                    localidad = it[Usuarios.localidad]
                )
            }
    }
}