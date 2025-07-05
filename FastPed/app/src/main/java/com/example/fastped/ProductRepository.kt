package com.example.fastped

import com.google.firebase.firestore.FirebaseFirestore
import com.example.fastped.Producto
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Trae sólo los productos con disponible == true
     */
    suspend fun getAvailableProducts(): List<Producto> {
        val snapshot = firestore.collection("productos")
            .whereEqualTo("disponible", true)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            Producto(
                id        = doc.getLong("id")?.toInt() ?: 0,
                nombre    = doc.getString("nombre") ?: "",
                disponible= doc.getBoolean("disponible") ?: false
            )
        }
    }
}