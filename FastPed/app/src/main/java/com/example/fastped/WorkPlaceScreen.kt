// WorkPlaceScreen.kt
package com.example.fastped

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fastped.model.Pedido
import com.example.fastped.model.PedidoProducto
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

// Combina un Pedido con sus productos
private data class PedidoConProductos(
    val pedido: Pedido,
    val productos: List<PedidoProducto>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPlaceScreen(
    nav: NavHostController,
    storeId: String,
    userDni: String
) {
    val db = Firebase.firestore

    var permissions      by remember { mutableStateOf<List<String>?>(null) }
    var isLoading        by remember { mutableStateOf(true) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }
    var waitingForRole   by remember { mutableStateOf(false) }
    var selectedTab      by remember { mutableStateOf(0) }

    // 1) Cargo roleId del miembro → 2) Cargo lista de permisos
    LaunchedEffect(storeId, userDni) {
        try {
            val memberSnap = db.collection("stores")
                .document(storeId)
                .collection("members")
                .document(userDni)
                .get()
                .await()

            // Si el doc existe pero no hay roleId asignado, esperamos
            val roleId = memberSnap.getString("roleId")
            if (roleId.isNullOrBlank()) {
                waitingForRole = true
                return@LaunchedEffect
            }

            // Si sí hay roleId, vamos por sus permisos
            val roleSnap = db.collection("stores")
                .document(storeId)
                .collection("roles")
                .document(roleId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            permissions = (roleSnap.get("permissions") as? List<*>)?.filterIsInstance<String>()
        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    // ------------- UI -------------
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Mostrar mensaje de “espera de rol”
    if (waitingForRole) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tu cuenta está pendiente de asignación de rol.\nPor favor espera unos minutos.\nSi esto continua comunicarse con el encargado.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }

    // Errores distintos a “espera”
    errorMsg?.let { msg ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $msg")
        }
        return
    }

    // Si pasamos todos los checks, construimos las pestañas según permisos
    // (idéntico al tu código anterior)
    val effectivePerms = if (permissions!!.contains("Administrador")) {
        listOf("Recepcionista","Cocinero","Despachador","Contabilidad","Administrador")
    } else {
        permissions!!
    }

    val tabs = effectivePerms.mapNotNull { perm ->
        when (perm) {
            "Recepcionista"  -> TabItem("Recepción",   Icons.Default.RoomService) { RecepcionistaScreen(storeId) }
            "Cocinero"       -> TabItem("Cocina",       Icons.Default.Kitchen)     { CocineroScreen(storeId, userDni) }
            "Despachador"    -> TabItem("Despacho",     Icons.Default.LocalShipping){ DespachadorScreen(storeId, userDni) }
            "Contabilidad"   -> TabItem("Contabilidad", Icons.Default.DateRange)    { ContabilidadScreen(storeId) }
            "Administrador"  -> TabItem("Admin",        Icons.Default.Settings)     { AdminScreen(storeId) }
            else             -> null
        }
    }.ifEmpty {
        listOf(TabItem("Sin acceso", Icons.Default.Block) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes permisos asignados")
            }
        })
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == selectedTab,
                        onClick  = { selectedTab = index },
                        icon     = { Icon(tab.icon, contentDescription = tab.title) },
                        label    = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            tabs[selectedTab].screen()
        }
    }
}

private data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable ()->Unit
)

@Composable
fun RecepcionistaScreen(storeId: String) {
    val db = Firebase.firestore

    // Agrupa un Pedido con sus productos
    data class PedidoConProductos(val pedido: Pedido, val productos: List<PedidoProducto>)

    var listaPedidos by remember { mutableStateOf<List<PedidoConProductos>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storeId) {
        try {
            // 1) Traer sólo los pedidos cuyo estado global sea "Pagado"
            val pedidosSnap = db.collection("orders")
                .whereEqualTo("estado", "Pagado")
                .get()
                .await()

            val resultado = mutableListOf<PedidoConProductos>()

            // 2) Para cada pedido, consulta SU sub-colección "productos" filtrando sólo por IDRes
            pedidosSnap.documents.mapNotNull { doc ->
                doc.toObject(Pedido::class.java)?.copy(idPedido = doc.id)
            }.forEach { pedido ->
                val prodsSnap = db.collection("orders")
                    .document(pedido.idPedido)
                    .collection("productos")
                    .whereEqualTo("IDRes", storeId)    // sólo por restaurante
                    .get()
                    .await()

                // Convertir cada doc a PedidoProducto
                val prods = prodsSnap.documents.mapNotNull { p ->
                    p.toObject(PedidoProducto::class.java)
                }
                if (prods.isNotEmpty()) {
                    resultado += PedidoConProductos(pedido, prods)
                }
            }

            listaPedidos = resultado
        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    // UI de carga / error / vacío / lista
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $errorMsg")
        }
        listaPedidos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pedidos pagados para esta tienda")
        }
        else -> LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listaPedidos) { pp ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Pedido: ${pp.pedido.idPedido}", style = MaterialTheme.typography.titleMedium)
                        Text("Cliente: ${pp.pedido.idCliente}")
                        Text("Fecha: ${pp.pedido.fechaCompra?.toDate()}")
                        Text("Total: S/ ${pp.pedido.total}")

                        Spacer(Modifier.height(8.dp))
                        Text("Productos:", style = MaterialTheme.typography.titleSmall)
                        pp.productos.forEach { prod ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(prod.NombreProducto, style = MaterialTheme.typography.bodyLarge)
                                    Text(prod.Descripcion,   style = MaterialTheme.typography.bodySmall)
                                }
                                Text("x${prod.Cantidad}")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // Marca el pedido global como "En espera"
                                db.collection("orders")
                                    .document(pp.pedido.idPedido)
                                    .update("estado", "En espera")
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Confirmar Recepción")
                        }
                    }
                }
            }
        }
    }
}

// —————————————————————————————————————————————————————————————————————————
// Placeholders para el resto; cuando lo completes, integra tu lógica:
@Composable fun CocineroScreen(storeId: String, userDni: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Cocina (pendiente)")
    }
}
@Composable fun DespachadorScreen(storeId: String, userDni: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Despacho (pendiente)")
    }
}
@Composable fun ContabilidadScreen(storeId: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Contabilidad (pendiente)")
    }
}
@Composable fun AdminScreen(storeId: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Administración (pendiente)")
    }
}
