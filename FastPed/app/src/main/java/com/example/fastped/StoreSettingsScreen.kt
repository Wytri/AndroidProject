// StoreSettingsScreen.kt
package com.example.fastped

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fastped.model.Rol
import com.example.fastped.model.UsuarioRestaurante
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsScreen(
    nav: NavHostController,
    storeId: String,
    currentDni: String,
    onBack: () -> Unit
) {
    val db    = Firebase.firestore
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    // — Determinar si es el dueño —
    var isOwner by remember { mutableStateOf(false) }
    LaunchedEffect(storeId) {
        val doc = db.collection("stores").document(storeId).get().await()
        if (doc.exists()) {
            isOwner = doc.getString("createdBy") == currentDni
        }
    }
    // — ESTADOS NUEVOS —
    var ruc         by remember { mutableStateOf("") }
    var rucError    by remember { mutableStateOf(false) }

    // Estados
    var codigo      by remember { mutableStateOf<String?>(null) }
    var roles       by remember { mutableStateOf<List<Rol>>(emptyList()) }
    var requests    by remember { mutableStateOf<List<String>>(emptyList()) }
    var members     by remember { mutableStateOf<List<UsuarioRestaurante>>(emptyList()) }
    var userDetails by remember { mutableStateOf<Map<String, Pair<String,String>>>(emptyMap()) }

    // Nuevo rol
    var showNewRoleDialog by remember { mutableStateOf(false) }
    var newRoleName       by remember { mutableStateOf("") }
    var newRoleDesc       by remember { mutableStateOf("") }
    var newRoleColor      by remember { mutableStateOf("#FF5722") }
    val allScreens        = listOf("Recepcionista", "Cocinero", "Despachador","Contabilidad","Administrador")
    var newRolePerms      by remember { mutableStateOf(setOf<String>()) }

    // Eliminar tienda
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pinConfirm       by remember { mutableStateOf("") }
    var pinError         by remember { mutableStateOf(false) }

    // Carga inicial
    LaunchedEffect(storeId) {
        // Código único
        db.collection("stores").document(storeId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    codigo = doc.getString("CodigoUnico")
                    ruc    = doc.getString("RUC") ?: ""    // ← cargamos el RUC
                    }
            }

        // Roles definidos
        db.collection("stores").document(storeId)
            .collection("roles")
            .addSnapshotListener { snap, _ ->
                roles = snap?.documents
                    ?.map { d ->
                        Rol(
                            IDRol          = d.id,
                            storeId        = storeId,
                            NombreRol      = d.getString("NombreRol").orEmpty(),
                            DescripcionRol = d.getString("DescripcionRol").orEmpty(),
                            ColorHex       = d.getString("ColorHex").orEmpty(),
                            createdBy      = d.getString("createdBy").orEmpty(),
                            createdAt      = d.getTimestamp("createdAt") ?: Timestamp.now(),
                            permissions    = d.get("permissions") as? List<String> ?: emptyList()
                        )
                    } ?: emptyList()
            }

        // Solicitudes pendientes
        db.collection("stores").document(storeId)
            .collection("workerRequests")
            .addSnapshotListener { snap, _ ->
                requests = snap?.documents?.map { it.id } ?: emptyList()
            }

        // Miembros (aprobados)
        db.collection("stores").document(storeId)
            .collection("members")
            .addSnapshotListener { snap, _ ->
                members = snap?.documents?.mapNotNull { d ->
                    d.getString("userId")?.let { uid ->
                        UsuarioRestaurante(
                            id        = d.id,
                            userId    = uid,
                            storeId   = storeId,
                            roleId    = d.getString("roleId").orEmpty(),
                            joinCode  = d.getString("joinCode").orEmpty(),
                            createdAt = d.getTimestamp("createdAt") ?: Timestamp.now(),
                            expiresAt = d.getTimestamp("expiresAt")
                        )
                    }
                } ?: emptyList()
                // Cargar nombre y apellido desde users
                members.forEach { m ->
                    scope.launch {
                        val doc = db.collection("users").document(m.userId).get().await()
                        if (doc.exists()) {
                            val nom = doc.getString("nombre").orEmpty()
                            val ape = doc.getString("apellido").orEmpty()
                            userDetails = userDetails + (m.userId to (nom to ape))
                        }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (isOwner) {
                    IconButton(onClick = { showNewRoleDialog = true }) {
                        Icon(Icons.Default.Build, contentDescription = "Administrar Roles")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar Tienda", tint = Color.Red)
                    }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewRoleDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Rol")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // — NUEVO: Card para editar RUC —
            if (isOwner) {
                item {
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text("RUC de la tienda", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = ruc,
                                onValueChange = { new ->
                                    ruc = new.filter { it.isDigit() }.take(11)
                                    rucError = false
                                },
                                label = { Text("RUC (11 dígitos)") },
                                singleLine = true,
                                isError = ruc.isNotEmpty() && ruc.length != 11,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (ruc.isNotEmpty() && ruc.length != 11) {
                                Text("El RUC debe tener exactamente 11 dígitos",
                                    color = MaterialTheme.colorScheme.error)
                            }
                            if (rucError) {
                                Text("Este RUC ya existe para otra tienda",
                                    color = MaterialTheme.colorScheme.error)
                            }

                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val clash = db.collection("stores")
                                            .whereEqualTo("RUC", ruc)
                                            .get()
                                            .await()
                                            .documents
                                            .any { it.id != storeId }
                                        if (clash) {
                                            rucError = true
                                        } else {
                                            db.collection("stores")
                                                .document(storeId)
                                                .update("RUC", ruc)
                                                .await()
                                            Toast.makeText(ctx,
                                                "RUC actualizado",
                                                Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = (ruc.length == 11)
                            ) {
                                Text("Guardar RUC")
                            }
                        }
                    }
                }
            }

            // Código Único
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Código Único", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(codigo ?: "—", fontSize = 18.sp)
                            Spacer(Modifier.width(16.dp))
                            IconButton({
                                scope.launch {
                                    // Generar nuevo y comprobar unicidad
                                    var nuevo: String
                                    do {
                                        nuevo = (1..6).map {
                                            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[Random.nextInt(36)]
                                        }.joinToString("")
                                    } while (!db.collection("stores")
                                            .whereEqualTo("CodigoUnico", nuevo)
                                            .limit(1)
                                            .get().await()
                                            .isEmpty)
                                    db.collection("stores").document(storeId)
                                        .update("CodigoUnico", nuevo).await()
                                    codigo = nuevo
                                    Toast.makeText(ctx, "Código regenerado", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerar Código")
                            }
                        }
                    }
                }
            }

            // Solicitudes Pendientes
            if (requests.isNotEmpty()) {
                item { Text("Solicitudes Pendientes", style = MaterialTheme.typography.titleMedium) }
                items(requests) { dni ->
                    Card {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dni, Modifier.weight(1f))
                            IconButton({
                                scope.launch {
                                    // Crear membresía
                                    val member = UsuarioRestaurante(
                                        id        = db.collection("stores")
                                            .document(storeId)
                                            .collection("members")
                                            .document(dni).id,
                                        userId    = dni,
                                        storeId   = storeId,
                                        roleId    = "",
                                        joinCode  = codigo.orEmpty(),
                                        createdAt = Timestamp.now()
                                    )
                                    // 1) Guardar en members
                                    db.collection("stores").document(storeId)
                                        .collection("members")
                                        .document(dni)
                                        .set(member.toMap()).await()
                                    // 2) Borrar solicitud
                                    db.collection("stores").document(storeId)
                                        .collection("workerRequests")
                                        .document(dni)
                                        .delete().await()
                                    // 3) Actualizar tiendaId en perfil
                                    db.collection("users").document(dni)
                                        .update("tiendaId", storeId).await()

                                    Toast.makeText(ctx, "Trabajador '$dni' aprobado 🎉", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Aceptar")
                            }
                            IconButton({
                                scope.launch {
                                    db.collection("stores").document(storeId)
                                        .collection("workerRequests")
                                        .document(dni)
                                        .delete().await()
                                    Toast.makeText(ctx, "Solicitud de '$dni' rechazada", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Rechazar")
                            }
                        }
                    }
                }
            }

            // Trabajadores Registrados
            item { Text("Trabajadores Registrados", style = MaterialTheme.typography.titleMedium) }
            items(members) { mem ->
                var expanded by remember { mutableStateOf(false) }
                val displayName = userDetails[mem.userId]?.let { "${it.first} ${it.second}" } ?: mem.userId
                Card {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(displayName)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (mem.roleId.isBlank()) "Sin rol"
                                else roles.find { it.IDRol == mem.roleId }?.NombreRol.orEmpty(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // Botón Editar rol
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Asignar rol")
                        }
                        // Menú para rol
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin rol") },
                                onClick = {
                                    scope.launch {
                                        db.collection("stores").document(storeId)
                                            .collection("members")
                                            .document(mem.userId)
                                            .update("roleId", "").await()
                                        expanded = false
                                        Toast.makeText(ctx, "Rol removido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            roles.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.NombreRol) },
                                    onClick = {
                                        scope.launch {
                                            db.collection("stores").document(storeId)
                                                .collection("members")
                                                .document(mem.userId)
                                                .update("roleId", r.IDRol).await()
                                            expanded = false
                                            Toast.makeText(ctx, "Rol asignado: ${r.NombreRol}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        // Botón Eliminar trabajador
                        IconButton(onClick = {
                            scope.launch {
                                // 1) Borrar de members
                                db.collection("stores").document(storeId)
                                    .collection("members")
                                    .document(mem.userId)
                                    .delete().await()
                                // 2) Borrar de workerRoles (opcional)
                                db.collection("stores").document(storeId)
                                    .collection("workerRoles")
                                    .document(mem.userId)
                                    .delete().await()
                                // 3) Actualizar usuario
                                db.collection("users").document(mem.userId)
                                    .update("tiendaId", null as String?).await()

                                Toast.makeText(ctx, "Trabajador '${mem.userId}' eliminado", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar trabajador", tint = Color.Red)
                        }
                    }
                }
            }

            // Roles Definidos
            item { Text("Roles Definidos", style = MaterialTheme.typography.titleMedium) }
            items(roles) { r ->
                Card {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(r.ColorHex)))
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.NombreRol, style = MaterialTheme.typography.bodyLarge)
                            Text(r.DescripcionRol, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({
                            scope.launch {
                                db.collection("stores").document(storeId)
                                    .collection("roles")
                                    .document(r.IDRol)
                                    .delete().await()
                                Toast.makeText(ctx, "Rol eliminado", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar rol")
                        }
                    }
                }
            }
        }
    }

    // Diálogo Nuevo Rol
    if (showNewRoleDialog) {
        AlertDialog(
            onDismissRequest = { showNewRoleDialog = false },
            title = { Text("Crear Nuevo Rol") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newRoleName,
                        onValueChange = { newRoleName = it },
                        label = { Text("Nombre del rol") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newRoleDesc,
                        onValueChange = { newRoleDesc = it },
                        label = { Text("Descripción") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Color", style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FF5722", "#4CAF50", "#2196F3", "#FFD600", "#9C27B0", "#607D8B").forEach { col ->
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(col)))
                                    .clickable { newRoleColor = col },
                                contentAlignment = Alignment.Center
                            ) {
                                if (newRoleColor == col) Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Permisos", style = MaterialTheme.typography.bodyMedium)
                    allScreens.forEach { screen ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = newRolePerms.contains(screen),
                                onCheckedChange = { checked ->
                                    newRolePerms = if (checked) newRolePerms + screen else newRolePerms - screen
                                }
                            )
                            Text(screen)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({
                    if (newRoleName.isNotBlank()) {
                        val id = db.collection("stores").document(storeId)
                            .collection("roles").document().id
                        val rol = Rol(
                            IDRol          = id,
                            storeId        = storeId,
                            NombreRol      = newRoleName.trim(),
                            DescripcionRol = newRoleDesc.trim(),
                            ColorHex       = newRoleColor,
                            createdBy      = currentDni,
                            permissions    = newRolePerms.toList()
                        )
                        scope.launch {
                            db.collection("stores").document(storeId)
                                .collection("roles").document(id)
                                .set(rol.toMap()).await()
                            showNewRoleDialog = false
                            newRoleName = ""
                            newRoleDesc = ""
                            newRolePerms = emptySet()
                            Toast.makeText(ctx, "Rol creado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton({ showNewRoleDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo Eliminar Tienda
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Tienda", color = Color.Red) },
            text = {
                Column {
                    Text("¿Seguro que quieres eliminar esta tienda?")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { pinConfirm = it; pinError = false },
                        label = { Text("Ingresa tu PIN") },
                        isError = pinError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true
                    )
                    if (pinError) Text("PIN incorrecto", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton({
                    scope.launch {
                        val userDoc = db.collection("users").document(currentDni).get().await()
                        if (userDoc.getString("pin") == pinConfirm) {
                            // Eliminar sub-colecciones
                            listOf("roles","workerRequests","members").forEach { sub ->
                                db.collection("stores").document(storeId)
                                    .collection(sub).get().await().documents.forEach { it.reference.delete() }
                            }
                            db.collection("stores").document(storeId).delete().await()
                            db.collection("users").document(currentDni)
                                .update("tiendaId", null as String?).await()
                            Toast.makeText(ctx, "Tienda eliminada", Toast.LENGTH_SHORT).show()
                            showDeleteDialog = false
                            nav.navigate("home") { popUpTo("login"){} }
                        } else {
                            pinError = true
                        }
                    }
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton({ showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}