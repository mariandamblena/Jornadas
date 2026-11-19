package ar.jornadas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.EstadoDescarga

@Composable
fun Portada(model: ProgramaModel) {
    var seccion by rememberSaveable { mutableStateOf("Programa") }
    var detalle by rememberSaveable { mutableStateOf<String?>(null) }
    var dia by rememberSaveable { mutableStateOf(0) }
    var tema by rememberSaveable { mutableStateOf("Todos") }
    val programa by model.programa.collectAsStateWithLifecycle()
    val estadoDescarga by model.estadoDescarga.collectAsStateWithLifecycle()
    val charlas = programa?.charlas.orEmpty()
    BackHandler(detalle != null || seccion != "Programa") {
        if (detalle != null) detalle = null else seccion = "Programa"
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF185B54), secondary = Color(0xFF995136))) {
        Scaffold(bottomBar = {
            if (detalle == null) NavigationBar {
                listOf("Programa" to "▤", "Mi agenda" to "☆", "Mesa" to "⚙").forEach { (nombre, icono) ->
                    NavigationBarItem(seccion == nombre, { seccion = nombre },
                        icon = { Text(icono) }, label = { Text(nombre) })
                }
            }
        }) { margen ->
            Column(Modifier.fillMaxSize().padding(margen).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("Jornadas del Sur", style = MaterialTheme.typography.labelLarge)
                if (seccion != "Mesa" && programa?.descargado == true &&
                    estadoDescarga == EstadoDescarga.Error
                ) {
                    Text("No se pudo actualizar el programa. Se están usando datos guardados.")
                }
                if (detalle != null) {
                    TextButton(onClick = { detalle = null }) { Text("Volver") }
                    val ficha = charlas.find { it.id == detalle }
                    if (ficha != null) Column(Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(ficha.titulo, style = MaterialTheme.typography.headlineMedium)
                        Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}")
                        Text(ficha.ponente, style = MaterialTheme.typography.titleMedium)
                        Text(ficha.resumen)
                        Button(onClick = { model.marcar(ficha.id) }) {
                            Text(if (ficha.favorita) "Quitar de mi agenda" else "Agregar a mi agenda")
                        }
                    } else if (programa?.descargado == true) {
                        Text("Esta charla ya no está en el programa.")
                    }
                } else {
                    Text(seccion, style = MaterialTheme.typography.headlineLarge)
                    when (seccion) {
                        "Programa" -> Catalogo(
                            programa = programa,
                            estadoDescarga = estadoDescarga,
                            dia = dia,
                            tema = tema,
                            elegirDia = { dia = it },
                            elegirTema = { tema = it },
                            recargar = model::recargar,
                            abrir = { detalle = it }
                        )
                        "Mi agenda" -> {
                            val guardadas = charlas.filter { it.favorita }
                            if (guardadas.isEmpty()) Text("Todavía no agregaste charlas.")
                            Grilla(guardadas) { detalle = it }
                        }
                        else -> Mesa(model.mesa)
                    }
                }
            }
        }
    }
}
