package com.example.fastped;

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


public class MainActivityInstrumentedTest {
    // 1) Regla para lanzar la Activity antes de cada test
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setupEmulators() {
        // Configurar SDK de Firebase para usar los puertos del Emulator
    }

    @Test
    fun testRegistroDeUsuarioMuestraBienvenida() {
        // Aquí irá tu test con Espresso
    }
}
