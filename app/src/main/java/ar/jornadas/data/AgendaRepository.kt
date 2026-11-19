package ar.jornadas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa: Flow<ProgramaGuardado> = dao.observar().map { filas ->
        ProgramaGuardado(filas.first().descargado, filas.mapNotNull { it.charla })
    }

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.guardarEdicion(fichas)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
