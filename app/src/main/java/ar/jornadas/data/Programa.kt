package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

data class ProgramaGuardado(val descargado: Boolean, val charlas: List<Charla>)

@Entity(tableName = "estado_programa")
data class EstadoPrograma(@PrimaryKey val id: Int = 1)

data class FilaPrograma(val descargado: Boolean, @Embedded val charla: Charla?)

@Dao
abstract class CarteleraDao {
    // La consulta devuelve el estado de la descarga aunque no haya charlas.
    @Query("""
        SELECT EXISTS(SELECT 1 FROM estado_programa WHERE id = 1) AS descargado, charlas.*
        FROM (SELECT 1) LEFT JOIN charlas ON 1 = 1
        ORDER BY dia, hora, sala
    """)
    abstract fun observar(): Flow<List<FilaPrograma>>

    @Query("SELECT id FROM charlas WHERE favorita = 1")
    protected abstract suspend fun favoritas(): List<String>

    @Query("DELETE FROM charlas")
    protected abstract suspend fun borrarCharlas()

    @Insert
    protected abstract suspend fun insertarCharlas(fichas: List<Charla>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun registrarDescarga(estado: EstadoPrograma)

    @Transaction
    open suspend fun guardarEdicion(fichas: List<Charla>) {
        val idsFavoritos = favoritas().toSet()
        borrarCharlas()
        insertarCharlas(fichas.map { it.copy(favorita = it.id in idsFavoritos) })
        registrarDescarga(EstadoPrograma())
    }

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)
}

@Database(entities = [Charla::class, EstadoPrograma::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}

val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS estado_programa (id INTEGER NOT NULL PRIMARY KEY)")
        db.execSQL("INSERT INTO estado_programa (id) SELECT 1 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}
