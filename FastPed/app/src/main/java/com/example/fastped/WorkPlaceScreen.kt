// WorkPlaceScreen.kt
package com.example.fastped
import com.google.firebase.Timestamp
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fastped.model.EstadosPedidoProducto
import com.example.fastped.model.Pedido
import com.example.fastped.model.PedidoProducto
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Combina un Pedido con sus productos
private data class PedidoConProductos(
    val pedido: Pedido,
    val productos: List<PedidoProducto>
)

@RequiresApi(Build.VERSION_CODES.O)
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
            // ── 0) Chequeo rápido: ¿es este userDni el createdBy de la tienda?
            val storeSnap = db.collection("stores")
                .document(storeId)
                .get()
                .await()
            val ownerDni = storeSnap.getString("createdBy")
            if (ownerDni == userDni) {
                // si es dueño, le damos “Administrador” y terminamos
                permissions = listOf("Administrador")
                return@LaunchedEffect
            }

            // ── 1) No es dueño → cargo roleId del miembro
            val memberSnap = db.collection("stores")
                .document(storeId)
                .collection("members")
                .document(userDni)
                .get()
                .await()

            val roleId = memberSnap.getString("roleId")
            if (roleId.isNullOrBlank()) {
                waitingForRole = true
                return@LaunchedEffect
            }

            // ── 2) Cargo permisos desde la colección roles
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
            "Contabilidad" -> TabItem(
                title  = "Contabilidad",
                icon   = Icons.Default.DateRange,
                screen = { ContabilidadScreen(storeId) }
            )
            "Administrador"  -> TabItem("Admin", Icons.Default.Settings)     { AdminScreen(nav, storeId, userDni) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecepcionistaScreen(storeId: String) {
    val db    = Firebase.firestore
    val scope = rememberCoroutineScope()

    var prodList  by remember { mutableStateOf<List<PedidoProducto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    // Lista de pedidos ya “movidos” para animar su salida
    val removedPedidos = remember { mutableStateListOf<String>() }

    LaunchedEffect(storeId) {
        try {
            // 1) traer todos los pedidos pagados
            val pedidosSnap = db.collection("orders")
                .whereEqualTo("estado", "Pagado")
                .get()
                .await()

            val lista = mutableListOf<PedidoProducto>()
            for (pedidoDoc in pedidosSnap.documents) {
                val pid = pedidoDoc.id
                val prodsSnap = db.collection("orders")
                    .document(pid)
                    .collection("productos")
                    .get()
                    .await()
                prodsSnap.documents
                    .mapNotNull { it.toObject(PedidoProducto::class.java) }
                    .filter {
                        it.IDRes == storeId &&
                                it.Estado == EstadosPedidoProducto.RECIBIDO
                    }
                    .also { lista += it }
            }
            prodList = lista
        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $errorMsg", color = MaterialTheme.colorScheme.error)
        }
        prodList.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay productos por procesar")
        }
        else -> {
            // agrupamos por pedido
            val grouped = prodList.groupBy { it.IDPedido }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                grouped.forEach { (pedidoId, productos) ->
                    item(key = pedidoId) {
                        AnimatedVisibility(
                            visible = pedidoId !in removedPedidos,
                            exit = fadeOut(animationSpec = tween(durationMillis = 300))
                        ) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Pedido: $pedidoId",
                                        style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))

                                    productos.forEach { prod ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(prod.NombreProducto,
                                                    style = MaterialTheme.typography.bodyLarge)
                                                Text("Cantidad: ${prod.Cantidad}",
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                // 1) actualizar todos los productos en Firestore
                                                productos.forEach { prod ->
                                                    db.collection("orders")
                                                        .document(prod.IDPedido)
                                                        .collection("productos")
                                                        .document(prod.IDProducto)
                                                        .update("Estado", EstadosPedidoProducto.EN_COLA)
                                                }
                                                // 2) marcar para animar salida y quitar del estado local
                                                removedPedidos += pedidoId
                                                prodList = prodList.filterNot { it.IDPedido == pedidoId }
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Poner todos en cola")
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
// —————————————————————————————————————————————————————————————————————————
// Placeholders para el resto; cuando lo completes, integra tu lógica:
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CocineroScreen(storeId: String, userDni: String) {
    val db    = Firebase.firestore
    val scope = rememberCoroutineScope()

    var enCola    by remember { mutableStateOf<List<PedidoProducto>>(emptyList()) }
    var enPrep    by remember { mutableStateOf<List<PedidoProducto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storeId) {
        isLoading = true
        errorMsg  = null
        try {
            // 1) Recuperamos TODOS los pedidos
            val ordersSnap = db.collection("orders")
                .get()
                .await()

            val colaList = mutableListOf<PedidoProducto>()
            val prepList = mutableListOf<PedidoProducto>()

            // 2) Por cada pedido, leemos SU subcolección productos y filtramos en memoria
            for (orderDoc in ordersSnap.documents) {
                val pid = orderDoc.id
                val prodsSnap = db.collection("orders")
                    .document(pid)
                    .collection("productos")
                    .get()
                    .await()

                val productos = prodsSnap.documents
                    .mapNotNull { it.toObject(PedidoProducto::class.java) }
                    // primero limitamos a TU tienda:
                    .filter { it.IDRes == storeId }

                // 3) Luego separamos por estado en Kotlin
                colaList += productos.filter {
                    it.Estado == EstadosPedidoProducto.EN_COLA
                }
                prepList += productos.filter {
                    it.Estado == EstadosPedidoProducto.EN_PREPARACION
                }
            }

            enCola = colaList
            enPrep = prepList
        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    // — UI —
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    errorMsg?.let { msg ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $msg", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // — En cola —
        Text("En cola", Modifier.padding(8.dp), style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = enCola,
                key   = { "${it.IDPedido}_${it.IDProducto}" }  // clave única
            ) { prod ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(prod.NombreProducto, style = MaterialTheme.typography.bodyLarge)
                            Text("Cantidad: ${prod.Cantidad}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            scope.launch {
                                db.collection("orders").document(prod.IDPedido)
                                    .collection("productos").document(prod.IDProducto)
                                    .update("Estado", EstadosPedidoProducto.EN_PREPARACION)
                                    .await()
                                enCola = enCola.filterNot {
                                    it.IDPedido == prod.IDPedido && it.IDProducto == prod.IDProducto
                                }
                                enPrep = enPrep + prod.copy(Estado = EstadosPedidoProducto.EN_PREPARACION)
                            }
                        }) {
                            Text("Preparar")
                        }
                    }
                }
            }
        }

        Divider()

        // — En preparación —
        Text("En preparación", Modifier.padding(8.dp), style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = enPrep,
                key   = { "${it.IDPedido}_${it.IDProducto}" }  // clave única
            ) { prod ->
                var showConfirm by remember { mutableStateOf(false) }

                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(prod.NombreProducto, style = MaterialTheme.typography.bodyLarge)
                            Text("Cantidad: ${prod.Cantidad}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { showConfirm = true }) {
                            Text("Listo")
                        }
                    }
                }

                if (showConfirm) {
                    AlertDialog(
                        onDismissRequest = { showConfirm = false },
                        title   = { Text("Confirmar") },
                        text    = { Text("Marcar '${prod.NombreProducto}' como listo para despacho?") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    db.collection("orders").document(prod.IDPedido)
                                        .collection("productos").document(prod.IDProducto)
                                        .update("Estado", EstadosPedidoProducto.LISTO_PARA_ENTREGAR)
                                        .await()
                                    enPrep = enPrep.filterNot {
                                        it.IDPedido == prod.IDPedido && it.IDProducto == prod.IDProducto
                                    }
                                }
                                showConfirm = false
                            }) {
                                Text("Sí")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirm = false }) {
                                Text("No")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespachadorScreen(storeId: String, userDni: String) {
    val db    = Firebase.firestore
    val scope = rememberCoroutineScope()

    // Modelo local para la UI
    data class DispatcherOrder(
        val pedidoId: String,
        val clienteId: String,
        val clienteNombre: String,
        val productos: List<PedidoProducto>
    )

    // Estado
    var ordersToDeliver by remember { mutableStateOf<List<DispatcherOrder>>(emptyList()) }
    var isLoading       by remember { mutableStateOf(true) }
    var errorMsg        by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storeId) {
        try {
            // 1) Traer todos los pedidos
            val headersSnap = db.collection("orders")
                .get()
                .await()

            val lista = mutableListOf<DispatcherOrder>()
            for (header in headersSnap.documents) {
                val pedidoId  = header.id
                val clienteId = header.getString("idcliente").orEmpty()

                // 2) Traer subcolección de productos y filtrar por tienda + listos para entregar
                val prodsSnap = db.collection("orders")
                    .document(pedidoId)
                    .collection("productos")
                    .get()
                    .await()
                val prods = prodsSnap.documents
                    .mapNotNull { it.toObject(PedidoProducto::class.java) }
                    .filter { it.IDRes == storeId && it.Estado == EstadosPedidoProducto.LISTO_PARA_ENTREGAR }

                if (prods.isNotEmpty()) {
                    // 3) Recuperar nombre del cliente
                    val userDoc = db.collection("users")
                        .document(clienteId)
                        .get()
                        .await()
                    val nom  = userDoc.getString("nombre").orEmpty()
                    val ape  = userDoc.getString("apellido").orEmpty()
                    val full = listOf(nom, ape).filter { it.isNotBlank() }.joinToString(" ")

                    lista += DispatcherOrder(
                        pedidoId       = pedidoId,
                        clienteId      = clienteId,
                        clienteNombre  = if (full.isNotBlank()) full else clienteId,
                        productos      = prods
                    )
                }
            }
            ordersToDeliver = lista
        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    // ── UI ──
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $errorMsg", color = MaterialTheme.colorScheme.error)
        }
        ordersToDeliver.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay pedidos listos para entregar")
        }
        else -> LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ordersToDeliver, key = { it.pedidoId }) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Pedido: ${order.pedidoId}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Cliente: ${order.clienteNombre} (${order.clienteId})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))

                        order.productos.forEach { prod ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(prod.NombreProducto, style = MaterialTheme.typography.bodyLarge)
                                    Text("Cantidad: ${prod.Cantidad}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    // 4a) Marcar cada producto como ENTREGADO
                                    order.productos.forEach { prod ->
                                        db.collection("orders")
                                            .document(order.pedidoId)
                                            .collection("productos")
                                            .document(prod.IDProducto)
                                            .update("Estado", EstadosPedidoProducto.ENTREGADO)
                                            .await()
                                    }
                                    // 4b) Actualizar estado global del pedido
                                    db.collection("orders")
                                        .document(order.pedidoId)
                                        .update("estado", "Completado")
                                        .await()
                                    // 4c) Eliminar de la lista local para refrescar la UI
                                    ordersToDeliver = ordersToDeliver
                                        .filterNot { it.pedidoId == order.pedidoId }
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Entregar pedido")
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContabilidadScreen(storeId: String) {
    val db   = Firebase.firestore
    val zone = ZoneId.systemDefault()
    val scope = rememberCoroutineScope()

    // — Calendario —
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis =
            selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    // — Estados internos —
    data class ContItem(
        val pedido: Pedido,
        val productos: List<PedidoProducto>,
        val pedidoId: String
    )

    var itemsByDay     by remember { mutableStateOf<List<ContItem>>(emptyList()) }
    var orderTotals    by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var dailyTotal     by remember { mutableStateOf(0.0) }
    var monthlyAverage by remember { mutableStateOf(0.0) }
    var isLoading      by remember { mutableStateOf(true) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }

    // — Efecto: recarga de datos al cambiar fecha o storeId —
    LaunchedEffect(storeId, selectedDate) {
        isLoading = true
        errorMsg  = null
        try {
            // 1) Traemos TODOS los pedidos COMPLETADOS
            val allCompleted = db.collection("orders")
                .whereEqualTo("estado", "Completado")
                .get()
                .await()

            // 2) Filtramos los de la fecha seleccionada
            val listaHoy   = mutableListOf<ContItem>()
            val mapaTotales = mutableMapOf<String, Double>()
            var sumaDia = 0.0

            allCompleted.documents.forEach { doc ->
                val pedido = doc.toObject(Pedido::class.java) ?: return@forEach
                val ts     = pedido.fechaCompra ?: return@forEach
                val ld     = ts.toDate().toInstant().atZone(zone).toLocalDate()
                if (ld == selectedDate) {
                    val pid   = doc.id
                    val total = pedido.total
                    // Traemos productos de esta tienda
                    val prodsSnap = doc.reference
                        .collection("productos")
                        .whereEqualTo("IDRes", storeId)
                        .get().await()
                    val prods = prodsSnap.documents
                        .mapNotNull { it.toObject(PedidoProducto::class.java) }
                    if (prods.isNotEmpty()) {
                        listaHoy += ContItem(pedido, prods, pid)
                        mapaTotales[pid] = total
                        sumaDia += total
                    }
                }
            }

            itemsByDay  = listaHoy
            orderTotals = mapaTotales
            dailyTotal  = sumaDia

            // 3) Calculamos el promedio mensual, filtrando en memoria
            val totPorDia = mutableMapOf<LocalDate, Double>()
            allCompleted.documents.forEach { doc ->
                val pedido = doc.toObject(Pedido::class.java) ?: return@forEach
                val ts     = pedido.fechaCompra ?: return@forEach
                val ld     = ts.toDate().toInstant().atZone(zone).toLocalDate()
                if (ld.year == selectedDate.year && ld.month == selectedDate.month) {
                    totPorDia[ld] = (totPorDia[ld] ?: 0.0) + pedido.total
                }
            }
            monthlyAverage = if (totPorDia.isEmpty()) 0.0 else totPorDia.values.average()

        } catch (e: Exception) {
            errorMsg = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    // — UI —
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Selector de fecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Calendario")
                Spacer(Modifier.width(8.dp))
                Text(selectedDate.toString())
            }
        }
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            selectedDate = Instant.ofEpochMilli(ms)
                                .atZone(zone)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            errorMsg != null -> Text(
                "Error: $errorMsg",
                color = MaterialTheme.colorScheme.error
            )
            itemsByDay.isEmpty() -> Text(
                "No hay ventas completas el $selectedDate",
                style = MaterialTheme.typography.bodyLarge
            )
            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(itemsByDay) { ci ->
                        val totalPedido = orderTotals[ci.pedidoId] ?: 0.0
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Pedido: ${ci.pedidoId}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                ci.productos.forEach { p ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(p.NombreProducto)
                                        Text("x${p.Cantidad}")
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Total pedido: ${"%.2f".format(totalPedido)}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Ganancia del día: ${"%.2f".format(dailyTotal)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Promedio mensual: ${"%.2f".format(monthlyAverage)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Convierte un timestamp en milisegundos a una fecha con formato "dd/MM/yyyy",
 * teniendo en cuenta la zona horaria local para evitar desfases.
 */
fun Long.convertMillisToDate(): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@convertMillisToDate
        // Ajustar por offset de zona y DST
        val offset = get(Calendar.ZONE_OFFSET) + get(Calendar.DST_OFFSET)
        add(Calendar.MILLISECOND, -offset)
    }
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(calendar.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    nav: NavHostController,
    storeId: String,
    currentDni: String
) {
    // Simplemente delega a StoreSettingsScreen, que ya contiene TODA la lógica de roles, RUC, etc.
    StoreSettingsScreen(
        nav = nav,
        storeId = storeId,
        currentDni = currentDni,
        onBack = { nav.popBackStack() }
    )
}
