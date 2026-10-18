package mx.tec.sipinna.viewmodel

// Reglas de la contraseña al crear una cuenta:
//   - De 10 a 72 caracteres (bytes, por bcrypt)
//   - Se permiten letras, números, símbolos y espacios
//   - Al menos una mayúscula
//   - Al menos un carácter especial (algo que no sea letra, número ni espacio: ! @ # $ % _ - ...)
//
// Ojo: esto es para ayudar al usuario en la app. El backend también debe validarlo,
// porque alguien podría llamar a la API sin usar la app.

const val CONTRASENA_MIN = 10

// El backend usa bcrypt, que solo acepta 72 bytes. Una letra normal ocupa 1 byte;
// las letras con acento, la ñ y los emojis ocupan 2 o más.
const val CONTRASENA_MAX = 72

private fun bytesContrasena(contrasena: String): Int =
    contrasena.toByteArray(Charsets.UTF_8).size

// Un requisito y si ya se cumple (para la lista con palomitas del registro)
data class RequisitoContrasena(val texto: String, val cumple: Boolean)

// Cuenta caracteres "reales": un emoji cuenta como 1 aunque internamente ocupe 2
fun largoContrasena(contrasena: String): Int =
    contrasena.codePointCount(0, contrasena.length)

private fun tieneMayuscula(contrasena: String): Boolean =
    contrasena.any { it.isUpperCase() }          // incluye Ñ, Á, É...

private fun tieneEspecial(contrasena: String): Boolean =
    contrasena.any { !it.isLetterOrDigit() && !it.isWhitespace() }

// Saltos de línea, tabuladores y otros caracteres invisibles no se permiten
// (el espacio normal sí)
private fun soloCaracteresPermitidos(contrasena: String): Boolean =
    contrasena.none { it.isISOControl() }

fun requisitosContrasena(contrasena: String): List<RequisitoContrasena> {
    val largo = largoContrasena(contrasena)
    return listOf(
        RequisitoContrasena(
            "Entre $CONTRASENA_MIN y $CONTRASENA_MAX caracteres",
            largo >= CONTRASENA_MIN && bytesContrasena(contrasena) <= CONTRASENA_MAX
        ),
        RequisitoContrasena("Al menos una letra mayúscula", tieneMayuscula(contrasena)),
        RequisitoContrasena("Al menos un carácter especial (ej. ! @ # \$ % _ -)", tieneEspecial(contrasena))
    )
}

fun contrasenaValida(contrasena: String): Boolean =
    soloCaracteresPermitidos(contrasena) && requisitosContrasena(contrasena).all { it.cumple }

// Mensaje para el aviso de error: dice lo primero que falta
fun errorDeContrasena(contrasena: String): String? {
    if (!soloCaracteresPermitidos(contrasena)) {
        return "La contraseña no puede tener saltos de línea ni tabuladores"
    }
    val largo = largoContrasena(contrasena)
    return when {
        largo < CONTRASENA_MIN -> "La contraseña debe tener al menos $CONTRASENA_MIN caracteres"
        bytesContrasena(contrasena) > CONTRASENA_MAX -> "La contraseña es demasiado larga (máximo $CONTRASENA_MAX caracteres; las letras con acento o ñ cuentan doble)"
        !tieneMayuscula(contrasena) -> "La contraseña debe tener al menos una letra mayúscula"
        !tieneEspecial(contrasena) -> "La contraseña debe tener al menos un carácter especial (ej. ! @ # \$ %)"
        else -> null
    }
}
