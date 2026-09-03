package com.example.sipinnaapp.viewmodel

data class EstadoRegistro(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,   // true mientras espera respuesta del servidor
    val error: String = "",
    val registroExitoso: Boolean = false
)
