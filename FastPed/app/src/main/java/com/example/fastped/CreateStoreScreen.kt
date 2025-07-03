// CreateStoreScreen.kt
package com.example.fastped

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fastped.model.Restaurante
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoreScreen(
    nav: NavHostController,
    ownerDni: String
) {
    val db        = Firebase.firestore
    val scope     = rememberCoroutineScope()
    val snackHost = remember { SnackbarHostState() }

    var name     by remember { mutableStateOf("") }
    var desc     by remember { mutableStateOf("") }
    var address  by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }

    var errName  by remember { mutableStateOf(false) }
    var errDesc  by remember { mutableStateOf(false) }
    var errAddr  by remember { mutableStateOf(false) }
    var errDist  by remember { mutableStateOf(false) }
    var errProv  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Crear Tienda") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // — Campos de texto —
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it; errName = false },
                label         = { Text("Nombre") },
                isError       = errName,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            if (errName) Text("Requerido", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = desc,
                onValueChange = { desc = it; errDesc = false },
                label         = { Text("Descripción") },
                isError       = errDesc,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            if (errDesc) Text("Requerido", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = address,
                onValueChange = { address = it; errAddr = false },
                label         = { Text("Dirección") },
                isError       = errAddr,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            if (errAddr) Text("Requerido", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value         = district,
                onValueChange = { district = it; errDist = false },
                label         = { Text("Distrito") },
                isError       = errDist,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            if (errDist) Text("Requerido", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value            = province,
                onValueChange    = { province = it; errProv = false },
                label            = { Text("Provincia") },
                isError          = errProv,
                singleLine       = true,
                keyboardOptions  = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier         = Modifier.fillMaxWidth()
            )
            if (errProv) Text("Requerido", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    // validación local
                    errName  = name.isBlank()
                    errDesc  = desc.isBlank()
                    errAddr  = address.isBlank()
                    errDist  = district.isBlank()
                    errProv  = province.isBlank()
                    if (listOf(errName, errDesc, errAddr, errDist, errProv).any { it }) return@Button

                    scope.launch {
                        runCatching {
                            // 1) Generar código único de 6 caracteres
                            var code: String
                            do {
                                code = (1..6)
                                    .map { "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[Random.nextInt(36)] }
                                    .joinToString("")
                                // verificar colisión
                            } while (!db.collection("stores")
                                    .whereEqualTo("CodigoUnico", code)
                                    .limit(1)
                                    .get()
                                    .await()
                                    .isEmpty)

                            // 2) Prepara el documento
                            val docRef = db.collection("stores").document()
                            val id     = docRef.id

                            // 3) Crea el objeto Restaurante
                            val store = Restaurante(
                                IDRes           = id,
                                CodigoUnico     = code,
                                Nombre          = name.trim(),
                                Descripcion     = desc.trim(),
                                Direccion       = address.trim(),
                                Distrito        = district.trim(),
                                Provincia       = province.trim(),
                                createdBy       = ownerDni,        // <-- DNI del dueño
                                createdAt       = Timestamp.now()  // <-- Timestamp de creación
                            )

                            // 4) Persiste la tienda
                            docRef.set(store.toMap()).await()

                            // 5) Asocia la tienda al usuario
                            db.collection("users")
                                .document(ownerDni)
                                .update("tiendaId", id)
                                .await()

                            // 6) Navega a MyStore
                            nav.navigate("myStore/$id") {
                                popUpTo("home") { inclusive = false }
                            }
                        }.onFailure {
                            snackHost.showSnackbar("Error: ${it.localizedMessage}")
                        }
                    }
                },
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Crear Tienda")
            }
        }
    }
}
