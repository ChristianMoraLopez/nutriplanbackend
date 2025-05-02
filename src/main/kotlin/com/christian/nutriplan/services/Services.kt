package com.christian.nutriplan.plugins

import com.christian.nutriplan.services.*
import org.jetbrains.exposed.sql.Database

data class Services(
    val usuarioService: UsuarioService,
    val categoriaIngredienteService: CategoriaIngredienteService,
    val ingredienteService: IngredienteService,
    val metodoPreparacionService: MetodoPreparacionService,
    val comidaService: ComidaService,
    val objetivoService: ObjetivoService,
    val menuService: MenuService,
    val seleccionIngredienteService: SeleccionIngredienteService
)

fun createServices(database: Database): Services {
    return Services(
        usuarioService = UsuarioService(database),
        categoriaIngredienteService = CategoriaIngredienteService(database),
        ingredienteService = IngredienteService(database),
        metodoPreparacionService = MetodoPreparacionService(database),
        comidaService = ComidaService(database),
        objetivoService = ObjetivoService(database),
        menuService = MenuService(database),
        seleccionIngredienteService = SeleccionIngredienteService(database)
    )
}