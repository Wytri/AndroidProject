// JoinStoreScreen.kt
package com.example.fastped

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinStoreScreen(
    nav: NavHostController,
    currentDni: String
) {
    val db   = Firebase.firestore
    val ctx  = LocalContext.current
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Registrarme en Tienda") })
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.trim().uppercase() },
                label = { Text("Código Único") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    loading = true
                    // 1) buscar tienda por código
                    db.collection("stores")
                        .whereEqualTo("CodigoUnico", code)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snap ->
                            if (snap.isEmpty) {
                                Toast.makeText(ctx, "Código no válido", Toast.LENGTH_SHORT).show()
                                loading = false
                            } else {
                                val storeId = snap.documents.first().id
                                // 2) crear solicitud en workerRequests
                                db.collection("stores")
                                    .document(storeId)
                                    .collection("workerRequests")
                                    .document(currentDni)
                                    .set(mapOf(
                                        "userId"    to currentDni,
                                        "joinCode"  to code,
                                        "createdAt" to Timestamp.now()
                                    ))
                                    .addOnSuccessListener {
                                        Toast.makeText(ctx,
                                            "Solicitud enviada. Espera aprobación.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        nav.popBackStack()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(ctx,
                                            "Error enviando solicitud",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .also { loading = false }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(ctx, "Error de red", Toast.LENGTH_SHORT).show()
                            loading = false
                        }
                },
                enabled = code.isNotBlank() && !loading
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Enviando..." else "Enviar solicitud")
            }
        }
    }
}