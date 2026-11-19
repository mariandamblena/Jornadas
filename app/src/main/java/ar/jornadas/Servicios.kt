package ar.jornadas

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.room.Room
import ar.jornadas.data.AgendaRepository
import ar.jornadas.data.Archivo
import ar.jornadas.data.MIGRACION_1_2
import ar.jornadas.data.ProgramaApi
import ar.jornadas.lab.ServicioLocal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class JornadasApp : Application() {
    val servicios: Servicios by lazy { Servicios(this) }
}

enum class EstadoDescarga { Inicial, Cargando, Completa, Error }

class Servicios(context: Context) {
    val mesa = ServicioLocal(context)
    private val archivo = Room.databaseBuilder(context, Archivo::class.java, "cartelera.db")
        .addMigrations(MIGRACION_1_2)
        .build()
    private val api = Retrofit.Builder().baseUrl("https://programa.invalid/")
        .client(OkHttpClient.Builder().addInterceptor(mesa).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ProgramaApi::class.java)
    val agenda = AgendaRepository(api, archivo.cartelera())

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var inicioSolicitado = false
    private val _estadoDescarga = MutableStateFlow(EstadoDescarga.Inicial)
    val estadoDescarga: StateFlow<EstadoDescarga> = _estadoDescarga.asStateFlow()

    @MainThread
    fun iniciar() {
        if (inicioSolicitado) return
        inicioSolicitado = true
        recargar()
    }

    @MainThread
    fun recargar() {
        if (_estadoDescarga.value == EstadoDescarga.Cargando) return
        _estadoDescarga.value = EstadoDescarga.Cargando

        alcance.launch {
            _estadoDescarga.value = try {
                agenda.sincronizar()
                EstadoDescarga.Completa
            } catch (cancelacion: CancellationException) {
                _estadoDescarga.value = EstadoDescarga.Inicial
                throw cancelacion
            } catch (error: Exception) {
                Log.w("Jornadas", "No se pudo actualizar el programa", error)
                EstadoDescarga.Error
            }
        }
    }
}
