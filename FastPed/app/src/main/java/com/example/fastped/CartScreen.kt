// CartScreen.kt
package com.example.fastped

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fastped.model.PedidoProducto
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
//import com.example.fastped.

// 1) RENOMBRAMOS el helper para no repetir nombres
private data class CartEntry(
    val docId: String,
    val producto: PedidoProducto
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    nav: NavHostController,
    currentDni: String
) {
    val db    = Firebase.firestore
    val scope = rememberCoroutineScope()

    // 2) Cambiamos el tipo de la lista
    var cartItems by remember { mutableStateOf<List<CartEntry>>(emptyList()) }
    var loading   by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentDni) {
        db.collection("users")
            .document(currentDni)
            .collection("cart")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    errorMsg = err.localizedMessage
                    loading = false
                    return@addSnapshotListener
                }
                cartItems = snap
                    ?.documents
                    ?.mapNotNull { d ->
                        d.toObject(PedidoProducto::class.java)
                            ?.let { prod -> CartEntry(d.id, prod) }
                    } ?: emptyList()
                loading = false
            }
    }

    val total = cartItems.sumOf { ci ->
        val p    = ci.producto
        val desc = p.Descuento ?: 0.0
        val pf   = p.PrecioUnitario * (1 - desc / 100.0)
        pf * p.Cantidad
    }
    // val total2 = PaymentUtils.calculateTotal(cartItems.map { it.producto })

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Carrito") }) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMsg != null -> Text(
                    "Error: $errorMsg",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
                cartItems.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.RemoveShoppingCart,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Tu carrito está vacío", color = Color.Gray)
                    }
                }
                else -> Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Productos:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3) Iteramos sobre CartEntry en lugar de CartItem
                        items(cartItems, key = { it.docId }) { ci ->
                            val prod = ci.producto
                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!prod.PhotoBase64.isNullOrEmpty()) {
                                        base64ToImageBitmap(prod.PhotoBase64!!)?.let { bmp ->
                                            Image(
                                                bitmap = bmp,
                                                contentDescription = prod.NombreProducto,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    } else {
                                        Box(
                                            Modifier
                                                .size(64.dp)
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(prod.NombreProducto, fontSize = 16.sp)
                                        val desc       = prod.Descuento ?: 0.0
                                        val precioFinal = prod.PrecioUnitario * (1 - desc / 100.0)
                                        val subtotal   = precioFinal * prod.Cantidad
                                        Text(
                                            if (desc > 0.0)
                                                "S/%.2f (%.0f%% off)".format(subtotal, desc)
                                            else
                                                "S/%.2f".format(subtotal),
                                            fontSize = 14.sp,
                                            color = if (desc > 0.0) Color(0xFFD32F2F) else Color.Unspecified
                                        )
                                        Text("Cantidad: ${prod.Cantidad}", fontSize = 13.sp)
                                        Text("Estado: ${prod.Estado}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            db.collection("users")
                                                .document(currentDni)
                                                .collection("cart")
                                                .document(ci.docId)
                                                .delete()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total:", fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Text(
                            "S/%.2f".format(total),
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            nav.navigate("checkout/$currentDni")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = cartItems.isNotEmpty()
                    ) {
                        Text("Seleccionar restaurante para pagar")
                    }
                }
            }
        }
    }
}
