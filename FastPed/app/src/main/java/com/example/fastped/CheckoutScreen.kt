// app/src/main/java/com/example/fastped/CheckoutScreen.kt
package com.example.fastped

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fastped.model.PedidoProducto
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
//import com.example.fastped.util.PaymentUtils

private data class CartItem(
    val docId: String,
    val producto: PedidoProducto
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    nav: NavHostController,
    currentDni: String
) {
    val db = Firebase.firestore

    var cartItems   by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var storeNames  by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var storeLogos  by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading     by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentDni) {
        try {
            // 1) Carga los ítems del carrito
            val snap = db.collection("users")
                .document(currentDni)
                .collection("cart")
                .get()
                .await()
            cartItems = snap.documents.mapNotNull { doc ->
                doc.toObject(PedidoProducto::class.java)
                    ?.let { prod -> CartItem(doc.id, prod) }
            }

            // 2) Recupera nombres y logos en Base64 de cada tienda
            val ids = cartItems.map { it.producto.IDRes }.toSet()
            val namesMap = mutableMapOf<String, String>()
            val logosMap = mutableMapOf<String, String>()
            ids.forEach { storeId ->
                val storeDoc = db.collection("stores")
                    .document(storeId)
                    .get()
                    .await()
                namesMap[storeId] = storeDoc.getString("Nombre") ?: storeId
                // Aquí tomamos tu campo StoreLogoBase64
                logosMap[storeId] = storeDoc.getString("StoreLogoBase64") ?: ""
            }
            storeNames = namesMap
            storeLogos = logosMap

        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Resumen de pedido") },
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMsg != null -> Text(
                    text = "Error: $errorMsg",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                cartItems.isEmpty() -> Text(
                    text = "No tienes ítems en el carrito",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val byStore = cartItems.groupBy { it.producto.IDRes }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        byStore.forEach { (storeId, items) ->
                            item {
                                val subtotalCents = items.sumOf { ci ->
                                    val p = ci.producto
                                    val desc = p.Descuento ?: 0.0
                                    val price = p.PrecioUnitario * (1 - desc / 100.0)
                                    (price * p.Cantidad * 100).toInt()
                                }

                                //val subtotal = PaymentUtils.calculateTotal(items.map { it.producto })
                                // si necesitas céntimos:
                                //val subtotalCents2 = (subtotal * 100).toInt()

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        // Foto y nombre de la tienda
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val base64 = storeLogos[storeId]
                                            if (!base64.isNullOrBlank()) {
                                                base64ToImageBitmap(base64)?.let { bmp ->
                                                    Image(
                                                        bitmap = bmp,
                                                        contentDescription = storeNames[storeId],
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Gray)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = storeNames[storeId] ?: storeId,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Total: S/%.2f".format(subtotalCents / 100.0),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                nav.navigate("payment/$currentDni/$storeId/$subtotalCents")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Pagar S/%.2f".format(subtotalCents / 100.0))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

