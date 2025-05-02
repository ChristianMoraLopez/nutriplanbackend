package com.christian.nutriplan

import com.christian.nutriplan.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()

    // Configuración de la base de datos
    val database = configureDatabase()

    // Creación de servicios
    val services = createServices(database)

    configureHTTP()
    configureSecurity(services.usuarioService)
    configureRouting(services)
}