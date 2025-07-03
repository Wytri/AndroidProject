package com.example.fastped.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.fastped.model.Usuario

/**
 * Data Access Object para la tabla de usuarios.
 */
@Dao
interface UsuarioDao {
    /**
     * Inserta o actualiza un usuario.
     * Si ya existe (mismo DNI), lo reemplaza.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usuario: Usuario): Long

    /**
     * Recupera el perfil completo del usuario por su DNI.
     * Devuelve un Flow para observar cambios en tiempo real.
     */
    @Query("SELECT * FROM usuarios WHERE dni = :dni LIMIT 1")
    fun getUsuarioByDni(dni: String): Flow<Usuario?>

    /**
     * Verifica credenciales comparando DNI y hash de PIN.
     * Retorna null si no coincide.
     */
    @Query("SELECT * FROM usuarios WHERE dni = :dni AND pinHash = :pinHash LIMIT 1")
    suspend fun login(dni: String, pinHash: String): Usuario?

    /**
     * Lista todos los usuarios (normalmente habrá uno solo en perfil).
     */
    @Query("SELECT * FROM usuarios")
    fun getAll(): Flow<List<Usuario>>

    /**
     * Elimina un usuario de la base de datos.
     */
    @Delete
    suspend fun delete(usuario: Usuario)
}
