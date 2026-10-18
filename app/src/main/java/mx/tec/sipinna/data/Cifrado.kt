/*
 * SIPINNA — Cifrado.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Cifrado AES-256-GCM con llave en el Android Keystore para datos sensibles (token de sesión).
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-15
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra textos sensibles (el token de sesión) antes de guardarlos en el teléfono.
 * La llave vive en el Android Keystore: no se puede leer ni copiar fuera del teléfono.
 * Si se desinstala la app, la llave se borra junto con los datos.
 */
object Cifrado {

    private const val ALIAS = "sipinna_sesion"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"
    private const val BYTES_IV = 12   // GCM usa un vector inicial de 12 bytes

    /** Obtiene la llave; la primera vez la crea */
    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    /** "eyJhbGci..." → "Qm9sYS..." (vector inicial + texto cifrado, en Base64) */
    fun cifrar(texto: String): String {
        if (texto.isEmpty()) return ""
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, llave())
        val datos = cifrador.iv + cifrador.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(datos, Base64.NO_WRAP)
    }

    /**
     * Regresa el texto original, o null si no se pudo descifrar
     * (por ejemplo, si el texto se guardó sin cifrar en una versión anterior)
     */
    fun descifrar(cifrado: String): String? {
        if (cifrado.isEmpty()) return ""
        return try {
            val datos = Base64.decode(cifrado, Base64.NO_WRAP)
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, datos, 0, BYTES_IV))
            String(cifrador.doFinal(datos, BYTES_IV, datos.size - BYTES_IV), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}