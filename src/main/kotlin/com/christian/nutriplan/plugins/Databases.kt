package com.christian.nutriplan.plugins

import org.jetbrains.exposed.sql.Database
import io.ktor.server.application.*
import org.postgresql.ds.PGSimpleDataSource
import java.nio.file.Files
import java.util.Properties

fun Application.configureDatabase(): Database {
    // 1. Obtener configuración de la base de datos
    val dbUrl = environment.config.property("database.url").getString()
    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()
    val dbDriver = environment.config.property("database.driver").getString()

    // 2. Cargar certificado SSL desde resources
    val rootCertStream = this::class.java.classLoader.getResourceAsStream("root.crt")
        ?: throw IllegalStateException("SSL certificate 'root.crt' not found in classpath")

    // 3. Crear archivo temporal con el certificado
    val tempCertFile = Files.createTempFile("yb_cert", ".crt").toFile().apply {
        writeBytes(rootCertStream.readAllBytes())
    }

    // 4. Configurar el DataSource manualmente
    val dataSource = PGSimpleDataSource().apply {
        setURL(dbUrl)
        user = dbUser
        password = dbPassword
        ssl = true
        sslmode = "verify-full"
        sslrootcert = tempCertFile.absolutePath // Usamos el File.absolutePath
    }

    // 5. Conectar usando el DataSource configurado
    return Database.connect(dataSource)
}