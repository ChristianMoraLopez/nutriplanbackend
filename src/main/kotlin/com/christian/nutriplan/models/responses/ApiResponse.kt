// src/main/kotlin/com/christian/nutriplan/models/responses/ApiResponse.kt
package com.christian.nutriplan.models.responses

import com.christian.nutriplan.models.Usuario
import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse<out T> {
    @Serializable
    data class Success<T>(
        val data: T,
        val message: String? = null
    ) : ApiResponse<T>()

    @Serializable
    data class Error(
        val message: String,
        val error: String? = null
    ) : ApiResponse<Nothing>()


    @Serializable
    data class LoginResponse(
        val token: String,
        val usuario: Usuario
    )
}