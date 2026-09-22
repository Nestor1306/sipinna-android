package com.example.sipinnaapp.viewmodel

data class EstadoLogin(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String = "",
    val loginExitoso: Boolean = false,
    val token: String = "",
    val nombreUsuario: String = "",
    val esAnonimo: Boolean = false
)