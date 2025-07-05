package com.example.fastped

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.threetenabp.AndroidThreeTen
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.fastped.Producto
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await


@RunWith(AndroidJUnit4::class)
class ProductRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var repo: ProductRepository

    @Before
    fun setup() {
        // Inicializar ThreeTen y apuntar al emulator
        AndroidThreeTen.init(androidx.test.core.app.ApplicationProvider
            .getApplicationContext())
        firestore = Firebase.firestore.apply {
            useEmulator("10.0.2.2", 8080)
        }
        repo = ProductRepository(firestore)

        // Poblar datos de prueba en Firestore Emulator
        val batch = firestore.batch()
        val col = firestore.collection("productos")
        val docs = listOf(
            DocumentoDatos(1, "Lomo Saltado", true),
            DocumentoDatos(2, "Ají de Gallina", false),
            DocumentoDatos(3, "Ceviche", true)
        )
        docs.forEach { d ->
            batch.set(col.document(d.id.toString()), mapOf(
                "id" to d.id,
                "nombre" to d.nombre,
                "disponible" to d.disponible
            ))
        }
        runBlocking {
            batch.commit().await()
            // Espera un momento para que el emulator indexe
            TimeUnit.MILLISECONDS.sleep(500)
        }
    }

    @After
    fun tearDown() {
        // Limpia la colección
        runBlocking {
            val all = firestore.collection("productos").get().await()
            all.documents.forEach { it.reference.delete().await() }
        }
    }

    @Test
    fun getAvailableProducts_debeSoloTraerLosDisponibles() = runBlocking {
        val disponibles: List<Producto> = repo.getAvailableProducts()
        // Esperamos sólo los IDs 1 y 3
        Assert.assertEquals(2, disponibles.size)
        Assert.assertTrue(disponibles.all { it.disponible })
        Assert.assertEquals(setOf(1,3), disponibles.map { it.id }.toSet())
    }

    private data class DocumentoDatos(val id: Int, val nombre: String, val disponible: Boolean)
}
