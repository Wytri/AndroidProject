// app/src/main/java/com/example/fastped/repository/AuthRepository.kt
package com.example.fastped

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    /** Intenta loguear con email (o DNI) + pin */
    suspend fun login(email: String, pin: String): Result<String> {
        return try {
            val cred = auth.signInWithEmailAndPassword(email, pin).await()
            Result.success(cred.user!!.uid)
        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    fun currentUid(): String? = auth.currentUser?.uid
}