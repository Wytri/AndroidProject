// app/src/main/java/com/example/fastped/OrderHistoryScreen.kt
package com.example.fastped

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fastped.model.Pedido
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    currentDni: String,
    nav: NavHostController
) {
    val db          = Firebase.firestore
    var pedidos     by remember { mutableStateOf<List<Pedido>>(emptyList()) }
    var loading     by remember { mutableStateOf(true) }
    var error       by remember { mutableStateOf<String?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(currentDni) {
        loading = true
        error   = null
        try {
            // 1) Obtén todos los pedidos de este cliente (sin orderBy)
            val snapshot = db.collection("orders")
                .whereEqualTo("idcliente", currentDni)
                .get()
                .await()

            // 2) Mapea y ordena en memoria por fechaCompra descendente
            pedidos = snapshot.documents
                .mapNotNull { it.toObject(Pedido::class.java) }
                .sortedByDescending { it.fechaCompra }

        } catch (e: Exception) {
            error = "Error al cargar pedidos: ${e.localizedMessage}"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Historial de Pedidos") },
                actions = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pedidos.isEmpty() -> {
                    Text(
                        "No tienes pedidos recientes",
                        Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn {
                        items(pedidos, key = { it.idPedido }) { pedido ->
                            Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable {
                                        nav.navigate("orderDetail/${pedido.idPedido}")
                                    }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Pedido #${pedido.idPedido.takeLast(6)}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Pago: ${pedido.medioDePago}")
                                    Text(
                                        "Fecha: ${
                                            pedido.fechaCompra
                                                ?.toDate()
                                                ?.let { dateFormatter.format(it) }
                                                ?: "-"
                                        }"
                                    )
                                    Text("Estado: ${pedido.estado}")
                                    Text("Total: S/%.2f".format(pedido.total))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}