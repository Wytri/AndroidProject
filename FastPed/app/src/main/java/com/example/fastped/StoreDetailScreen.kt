// StoreDetailScreen.kt
package com.example.fastped

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fastped.model.Producto
import com.example.fastped.model.Restaurante
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    nav: NavHostController,
    storeId: String,
    currentDni: String
) {
    val db = com.google.firebase.ktx.Firebase.firestore
    var store by remember { mutableStateOf<Restaurante?>(null) }
    var products by remember { mutableStateOf<List<Producto>>(emptyList()) }

    var selected by remember { mutableStateOf<Producto?>(null) }
    var qty by remember { mutableStateOf(1) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(storeId) {
        try {
            val storeDoc = db.collection("stores").document(storeId).get().await()
            store = storeDoc.toObject(Restaurante::class.java)?.copy(IDRes = storeId)

            val prods = db.collection("stores").document(storeId)
                .collection("products").get().await()
                .documents.mapNotNull { d ->
                    d.toObject(Producto::class.java)?.copy(IDRes = storeId, IDProducto = d.id)
                }
            products = prods
        } catch (e: Exception) {
            Toast.makeText(nav.context, "Error cargando tienda: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(store?.Nombre ?: "Tienda") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (store == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Banner
            store?.BannerBase64?.let { b64 ->
                base64ToImageBitmap(b64)?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = "Banner tienda",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Logo
            store?.StoreLogoBase64?.let { b64 ->
                base64ToImageBitmap(b64)?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = "Logo tienda",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Datos generales
            Text(store?.Nombre ?: "-", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(store?.Descripcion ?: "-", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${store?.Direccion ?: ""}, ${store?.Distrito ?: ""}, ${store?.Provincia ?: ""}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))
            Text("Productos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(products, key = { it.IDProducto }) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = prod
                                qty = 1
                                showDialog = true
                            },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Imagen de producto si existe
                            if (!prod.PhotoBase64.isNullOrEmpty()) {
                                base64ToImageBitmap(prod.PhotoBase64!!)?.let { bmp ->
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = prod.NombreProducto,
                                        contentScale = ContentScale.Crop,
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
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(prod.NombreProducto, fontSize = 16.sp)
                                Text("S/%.2f".format(prod.Precio), style = MaterialTheme.typography.bodySmall)
                                if ((prod.Descuento ?: 0.0) > 0.0)
                                    Text("Descuento: ${prod.Descuento?.toInt() ?: 0}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton({
                                selected = prod
                                qty = 1
                                showDialog = true
                            }) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        // Dialog para seleccionar cantidad y agregar al carrito
        if (showDialog && selected != null) {
            val prod = selected!!
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(prod.NombreProducto) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Precio unitario: S/%.2f".format(prod.Precio))
                        Spacer(Modifier.height(8.dp))
                        if ((prod.Descuento ?: 0.0) > 0.0)
                            Text("Descuento: ${prod.Descuento?.toInt() ?: 0}%")
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button({ if (qty > 1) qty-- }, enabled = qty > 1) { Text("-") }
                            Spacer(Modifier.width(16.dp))
                            Text(qty.toString(), fontSize = 18.sp)
                            Spacer(Modifier.width(16.dp))
                            Button({ qty++ }) { Text("+") }
                        }
                        Spacer(Modifier.height(12.dp))
                        val finalPrice = prod.Precio * (1 - (prod.Descuento ?: 0.0) / 100.0)
                        Text("Total: S/%.2f".format(finalPrice * qty), style = MaterialTheme.typography.titleMedium)
                    }
                },
                confirmButton = {
                    TextButton({
                        if (currentDni.isBlank()) {
                            Toast.makeText(nav.context, "Debes estar logueado para agregar al carrito", Toast.LENGTH_SHORT).show()
                        } else {
                            // Guarda todos los datos necesarios en el carrito
                            val cartData = mapOf(
                                "IDRes"          to storeId,
                                "IDProducto"     to prod.IDProducto,
                                "NombreProducto" to prod.NombreProducto,
                                "Descripcion"    to prod.Descripcion,
                                "Cantidad"       to qty,
                                "PrecioUnitario" to prod.Precio,
                                "Descuento"      to (prod.Descuento ?: 0.0),
                                "PhotoBase64"    to prod.PhotoBase64,
                                "Estado"         to "Pendiente a pagar"
                            )
                            db.collection("users")
                                .document(currentDni)
                                .collection("cart")
                                .document("${storeId}_${prod.IDProducto}")
                                .set(cartData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        nav.context,
                                        "Agregado al carrito: $qty x ${prod.NombreProducto}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        showDialog = false
                    }) {
                        Text("Agregar")
                    }
                },
                dismissButton = {
                    TextButton({ showDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}


