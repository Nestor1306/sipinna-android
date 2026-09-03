package com.example.sipinnaapp.viewmodel

data class EstadoLogin(
    val email: String = "",
    val password: String = "",
    val error: String = "",
    val loginExitoso: Boolean = false
)
