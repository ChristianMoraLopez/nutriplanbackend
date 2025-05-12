package com.christian.nutriplan.plugins

import com.christian.nutriplan.services.*
import org.jetbrains.exposed.sql.Database

data class Services(
    val usuarioService: UsuarioService,
    val categoriaIngredienteService: CategoriaIngredienteService,
    val ingredienteService: IngredienteService,
    val tipoComidaService: TipoComidaService,
    val metodoPreparacionService: MetodoPreparacionService,
    val recetaService: RecetaService,
    val recetaGuardadaService: RecetaGuardadaService,
    val recetaIngredienteService: RecetaIngredienteService
)

fun createServices(database: Database): Services {
    return Services(
        usuarioService = UsuarioService(database),
        categoriaIngredienteService = CategoriaIngredienteService(database),
        ingredienteService = IngredienteService(database),
        tipoComidaService = TipoComidaService(database),
        metodoPreparacionService = MetodoPreparacionService(database),
        recetaService = RecetaService(database),
        recetaGuardadaService = RecetaGuardadaService(database),
        recetaIngredienteService = RecetaIngredienteService(database)
    )
}