package com.example.fastped

import com.jakewharton.threetenabp.AndroidThreeTen
import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)  // 👈 Inicializa la librería
    }
}