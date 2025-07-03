package com.example.fastped.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database
import com.example.fastped.dao.UsuarioDao
import com.example.fastped.model.Usuario

@Database(
    entities = [Usuario::class],
    version = 1,
    exportSchema = false
)
abstract class Appdata : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile private var INSTANCE: Appdata? = null

        fun getInstance(context: Context): Appdata =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                Appdata::class.java,
                "fastped.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}
