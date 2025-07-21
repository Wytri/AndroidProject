package com.example.fastped

import androidx.compose.foundation.Image
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
import com.example.fastped.model.PedidoProducto
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    nav: NavHostController
) {
    val db = Firebase.firestore
    var productos by remember { mutableStateOf<List<PedidoProducto>>(emptyList()) }
    var pedido by remember { mutableStateOf<Pedido?>(null) }
    // Mapa de storeId -> nombre de la tienda
    var storeNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    // 1) Cargar pedido y productos
    LaunchedEffect(orderId) {
        try {
            val pedidoDoc = db.collection("orders").document(orderId).get().await()
            pedido = pedidoDoc.toObject(Pedido::class.java)
            db.collection("orders").document(orderId)
                .collection("productos")
                .addSnapshotListener { snap, _ ->
                    productos = snap?.documents
                        ?.mapNotNull { it.toObject(PedidoProducto::class.java) }
                        ?: emptyList()
                    loading = false
                }
        } catch (_: Exception) {
            loading = false
        }
    }

    // 2) Obtener nombres de tiendas usando IDRes
    LaunchedEffect(productos) {
        val ids = productos.map { it.IDRes }.toSet()
        val map = mutableMapOf<String, String>()
        ids.forEach { storeId ->
            try {
                val doc = db.collection("stores").document(storeId).get().await()
                map[storeId] = doc.getString("Nombre") ?: "Desconocido"
            } catch (_: Exception) {
                map[storeId] = "Desconocido"
            }
        }
        storeNames = map
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle del Pedido") },
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
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Información general del pedido
                pedido?.let { p ->
                    Text("Pedido #${p.idPedido.takeLast(6)}", fontWeight = FontWeight.SemiBold)
                    Text("Fecha: ${p.fechaCompra?.toDate()}")
                    Text("Total: S/%.2f".format(p.total))
                    Text("Estado General: ${p.estado}")
                }
                Spacer(Modifier.height(12.dp))
                Text("Productos:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(productos) { prod ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Foto del producto
                                prod.PhotoBase64
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let { base64ToImageBitmap(it) }
                                    ?.let { bmp ->
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = prod.NombreProducto,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }

                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.fillMaxWidth()) {
                                    // Nombre y estado del producto
                                    Text(prod.NombreProducto, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Estado: ${prod.Estado}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    // Nombre del restaurante
                                    Text(
                                        "Restaurante: ${storeNames[prod.IDRes] ?: "Desconocido"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(Modifier.height(4.dp))
                                    // Cantidad y costo
                                    Text("Cantidad: ${prod.Cantidad}")
                                    val desc = prod.Descuento ?: 0.0
                                    val precioFinal = prod.PrecioUnitario * (1 - desc / 100.0)
                                    val costoTotal = precioFinal * prod.Cantidad
                                    Text(
                                        "Costo total: S/%.2f".format(costoTotal),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
