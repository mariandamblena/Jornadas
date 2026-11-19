package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ar.jornadas.JornadasApp
import ar.jornadas.data.ProgramaGuardado
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgramaModel(app: Application) : AndroidViewModel(app) {
    private val servicios = (app as JornadasApp).servicios
    private val repo = servicios.agenda
    val mesa = servicios.mesa
    val estadoDescarga = servicios.estadoDescarga
    val programa: StateFlow<ProgramaGuardado?> =
        repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun iniciar() = servicios.iniciar()

    fun recargar() = servicios.recargar()

    fun marcar(id: String) {
        viewModelScope.launch { repo.cambiarFavorita(id) }
    }
}
