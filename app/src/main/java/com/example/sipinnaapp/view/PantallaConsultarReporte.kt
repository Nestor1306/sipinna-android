package com.example.sipinnaapp.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.ui.theme.*

// Lo que se muestra cuando la consulta encontró el reporte.
// Viene de GET /public/report/{folio}?clave=... (solo trae folio, estado público y fechas).
data class ResultadoConsulta(
    val folio: String,
    val estado: String,            // estado público: recibido, en_revision, canalizado, en_atencion, concluido
    val creadoEl: String?,         // created_at
    val actualizadoEl: String?     // state_changed_at
)

// Consultar un reporte sin cuenta, con el folio y la clave de consulta que se dieron al enviarlo.
// La pantalla no hace la petición: avisa con alConsultar y muestra lo que le pasen
// (cargando, error o resultado). La petición vive en un ViewModel.
@Composable
fun PantallaConsultarReporte(
    folio: String,
    clave: String,
    alCambiarFolio: (String) -> Unit,
    alCambiarClave: (String) -> Unit,
    alConsultar: () -> Unit,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    error: String = "",                    // por ejemplo "No encontramos un reporte con esos datos."
    resultado: ResultadoConsulta? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = TextoPrincipal)
        }

        Text("Consultar un reporte", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        Espacio(8.dp)
        Text(
            text = "Escribe el folio y la clave de consulta que recibiste al enviar tu reporte. No necesitas cuenta.",
            fontSize = 14.sp,
            color = GrisTexto
        )

        Espacio(24.dp)

        CampoConIcono(
            etiqueta = "Folio",
            valor = folio,
            alCambiar = alCambiarFolio,
            icono = Icons.Default.Tag,
            ayuda = "RIETI-ATIZAPAN-2026-000001"
        )
        Espacio(16.dp)
        CampoConIcono(
            etiqueta = "Clave de consulta",
            valor = clave,
            alCambiar = alCambiarClave,
            icono = Icons.Default.Key,
            ayuda = "La clave que te dio la app al enviar"
        )

        if (error.isNotEmpty()) {
            Espacio(12.dp)
            Text(error, fontSize = 13.sp, color = RojoNoApto)
        }

        Espacio(24.dp)

        if (cargando) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = Teal)
            }
        } else {
            BotonPrincipal(
                texto = "Consultar",
                alPresionar = alConsultar,
                habilitado = folio.isNotBlank() && clave.isNotBlank()
            )
        }

        // Resultado: estado público y fechas (sin ubicación, fotos ni datos personales)
        resultado?.let { r ->
            Espacio(24.dp)
            val info = infoDeEstado(r.estado)
            TarjetaResumen {
                EtiquetaGris("Folio")
                Espacio(2.dp)
                Text(r.folio, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                Espacio(12.dp)
                Text(info.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = info.color)
                if (info.detalle.isNotEmpty()) {
                    Espacio(4.dp)
                    Text(info.detalle, fontSize = 12.sp, color = GrisTexto)
                }
                Espacio(12.dp)
                Text("Enviado: ${fechaLegible(r.creadoEl)}", fontSize = 12.sp, color = GrisTexto)
                Text("Última actualización: ${fechaLegible(r.actualizadoEl)}", fontSize = 12.sp, color = GrisTexto)
            }
        }

        Espacio(24.dp)
    }
}

// Vista previa para Android Studio (no se usa en la app)
@Preview(showBackground = true)
@Composable
private fun VistaPreviaConsultarReporte() {
    SipinnaAppTheme {
        PantallaConsultarReporte(
            folio = "RIETI-ATIZAPAN-2026-000001",
            clave = "8f2c1d4e-3b6a-4f0e-9c7d-2a1b5e6f7a8b",
            alCambiarFolio = {},
            alCambiarClave = {},
            alConsultar = {},
            alRegresar = {},
            resultado = ResultadoConsulta(
                folio = "RIETI-ATIZAPAN-2026-000001",
                estado = "en_revision",
                creadoEl = "2026-10-15T14:30:00Z",
                actualizadoEl = "2026-10-16T09:05:00Z"
            )
        )
    }
}
