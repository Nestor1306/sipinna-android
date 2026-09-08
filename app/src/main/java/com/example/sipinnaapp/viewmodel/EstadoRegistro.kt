package com.example.sipinnaapp.viewmodel



data class EstadoRegistro(
    val nombre: String = "",
    val apellido: String = "",
    val genero: String = "",
    val edad: String = "",
    val telefono: String = "",
    val email: String = "",
    val password: String = "",
    val confirmarPassword:String = "",

    //control de pantalla

    val cargando: Boolean = false,   // true mientras espera respuesta del servidor
    val error: String = "",
    val registroExitoso: Boolean = false
)
