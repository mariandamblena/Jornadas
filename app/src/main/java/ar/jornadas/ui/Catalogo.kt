package ar.jornadas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.jornadas.EstadoDescarga
import ar.jornadas.data.Charla
import ar.jornadas.data.ProgramaGuardado

@Composable
fun Catalogo(
    programa: ProgramaGuardado?,
    estadoDescarga: EstadoDescarga,
    dia: Int,
    tema: String,
    elegirDia: (Int) -> Unit,
    elegirTema: (String) -> Unit,
    recargar: () -> Unit,
    abrir: (String) -> Unit
) {
    Selector(listOf("Ambos días", "Día 1", "Día 2"), dia, elegirDia)
    val temas = listOf("Todos", "Android", "Datos", "Calidad")
    Selector(temas, temas.indexOf(tema)) { elegirTema(temas[it]) }

    val cargando = estadoDescarga == EstadoDescarga.Cargando
    val falloSinDatos = estadoDescarga == EstadoDescarga.Error && programa?.descargado == false
    TextButton(onClick = recargar, enabled = !cargando) {
        Text(if (falloSinDatos) "Reintentar" else "Actualizar")
    }
    if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth())

    if (falloSinDatos) Text("No se pudo consultar el programa.")
    if (programa?.descargado == true) {
        val visibles = programa.charlas.filter {
            (dia == 0 || it.dia == dia) && (tema == "Todos" || it.tema == tema)
        }
        when {
            programa.charlas.isEmpty() -> Text("Sin charlas programadas.")
            visibles.isEmpty() -> Text("No hay charlas con estos filtros.")
            else -> Grilla(visibles, abrir)
        }
    }
}

@Composable
private fun Selector(opciones: List<String>, indice: Int, elegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEachIndexed { i, texto ->
            FilterChip(selected = i == indice, onClick = { elegir(i) }, label = { Text(texto) })
        }
    }
}

@Composable
fun Grilla(fichas: List<Charla>, abrir: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)) {
        items(fichas, key = { it.id }) { ficha ->
            ElevatedCard(onClick = { abrir(ficha.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}", style = MaterialTheme.typography.labelMedium)
                    Text(ficha.titulo, style = MaterialTheme.typography.titleLarge)
                    Text(if (ficha.favorita) "${ficha.tema} · En mi agenda" else ficha.tema)
                }
            }
        }
    }
}
