// ProfileScreen.kt
package com.example.fastped

import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.fastped.model.Usuario
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    nav: NavHostController,
    initial: Usuario,
    onSave: (Usuario) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // estados de formulario
    var email       by remember { mutableStateOf(initial.email) }
    var nombre      by remember { mutableStateOf(initial.nombre) }
    var apellido    by remember { mutableStateOf(initial.apellido) }
    var celular     by remember { mutableStateOf(initial.celular) }
    var tipoUsuario by remember { mutableStateOf(initial.tipo) }
    var sexo        by remember { mutableStateOf(initial.sexo) }
    var tipoError   by remember { mutableStateOf(false) }
    var sexoError   by remember { mutableStateOf(false) }

    // foto
    var photoBase64 by remember { mutableStateOf(initial.photoBase64) }
    var photoUri: Uri? by remember { mutableStateOf(null) }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            photoBase64 = null
        }
    }

    // PIN oculta
    var showPinDialog   by remember { mutableStateOf(false) }
    var pinConfirm      by remember { mutableStateOf("") }
    var pinConfirmError by remember { mutableStateOf(false) }

    val tiposList = listOf("Elige tipo", "Cliente", "Dueño de tienda", "Trabajador")
    val sexoList  = listOf("Elige sexo", "Hombre", "Mujer", "Prefiero no decir")

    // drawer
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menú", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

                // Perfil
                DrawerItem("Información de mi perfil", Icons.Default.Person) {
                    scope.launch { drawerState.close() }
                    nav.navigate("profile/${initial.dni}")
                }
                Divider()

                // Historial / favoritos
                DrawerItem("Historial de pedidos", Icons.Default.History) {
                    scope.launch { drawerState.close() }
                    nav.navigate("orderHistory")
                }
                DrawerItem("Tiendas favoritas", Icons.Default.Favorite) {
                    scope.launch { drawerState.close() }
                    nav.navigate("favoriteStores")
                }
                Divider()

                // Opciones de tienda según tipoUsuario
                if (tipoUsuario == 2) {
                    // Dueño
                    if (initial.tiendaId.isNullOrEmpty()) {
                        DrawerItem("Crear mi tienda", Icons.Default.Store) {
                            scope.launch { drawerState.close() }
                            nav.navigate("createStore/${initial.dni}")
                        }
                    } else {
                        DrawerItem("Mi tienda", Icons.Default.Storefront) {
                            scope.launch {
                                drawerState.close()
                                // Validar que quien pulsa sea realmente el creador:
                                val doc = Firebase.firestore
                                    .collection("stores")
                                    .document(initial.tiendaId)
                                    .get()
                                    .await()
                                val ownerDni = doc.getString("createdBy")
                                if (ownerDni == initial.dni) {
                                    nav.navigate("myStore/${initial.tiendaId}")
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Solo el propietario puede editar esta tienda",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    Divider()
                }
                if (tipoUsuario == 3) {
                    // Trabajador
                    if (initial.tiendaId.isNullOrEmpty()) {
                        DrawerItem("Registrarme en tienda", Icons.Default.GroupAdd) {
                            scope.launch { drawerState.close() }
                            nav.navigate("joinStore/${initial.dni}")
                        }
                    } else {
                        DrawerItem("Mi lugar de trabajo", Icons.Default.Work) {
                            scope.launch { drawerState.close() }
                            nav.navigate("workPlace/${initial.tiendaId}")
                        }
                    }
                    Divider()
                }

                Spacer(Modifier.weight(1f))
                DrawerItem("Cerrar sesión", Icons.Default.Logout) {
                    scope.launch { drawerState.close() }
                    onLogout()
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    title = { Text("Mi Perfil") },
                    actions = {
                        IconButton(onClick = {
                            tipoError = tipoUsuario == 0
                            sexoError = sexo == 0
                            if (!tipoError && !sexoError) {
                                showPinDialog = true
                                pinConfirm = ""
                                pinConfirmError = false
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar cambios")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // avatar
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { pickLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        photoUri != null -> Image(
                            painter = rememberAsyncImagePainter(photoUri),
                            contentDescription = "Avatar nuevo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        !photoBase64.isNullOrEmpty() -> {
                            base64ToImageBitmap(photoBase64)?.let { bmp ->
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Avatar guardado",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            } ?: Image(
                                painter = painterResource(R.drawable.ic_launcher_background),
                                contentDescription = "Avatar por defecto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        else -> Image(
                            painter = painterResource(R.drawable.ic_launcher_background),
                            contentDescription = "Avatar por defecto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // DNI
                Text("DNI: ${initial.dni}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))

                // campos
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = celular,
                    onValueChange = { celular = it },
                    label = { Text("Celular") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // tipoUsuario dropdown
                var expandedTipo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = !expandedTipo }
                ) {
                    OutlinedTextField(
                        value = tiposList[tipoUsuario],
                        onValueChange = {},
                        readOnly = true,
                        isError = tipoError,
                        label = { Text("Tipo de usuario") },
                        trailingIcon = { TrailingIcon(expandedTipo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        tiposList.forEachIndexed { i, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    tipoUsuario = i
                                    tipoError = i == 0
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
                if (tipoError) Text("Selecciona un tipo", color = MaterialTheme.colorScheme.error)

                Spacer(Modifier.height(16.dp))

                // sexo dropdown
                var expandedSexo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedSexo,
                    onExpandedChange = { expandedSexo = !expandedSexo }
                ) {
                    OutlinedTextField(
                        value = sexoList[sexo],
                        onValueChange = {},
                        readOnly = true,
                        isError = sexoError,
                        label = { Text("Sexo") },
                        trailingIcon = { TrailingIcon(expandedSexo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSexo,
                        onDismissRequest = { expandedSexo = false }
                    ) {
                        sexoList.forEachIndexed { i, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    sexo = i
                                    sexoError = i == 0
                                    expandedSexo = false
                                }
                            )
                        }
                    }
                }
                if (sexoError) Text("Selecciona un sexo", color = MaterialTheme.colorScheme.error)

                Spacer(Modifier.height(24.dp))

                // PIN dialog
                if (showPinDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showPinDialog = false
                            pinConfirm = ""
                            pinConfirmError = false
                        },
                        title = { Text("Confirma tu PIN") },
                        text = {
                            OutlinedTextField(
                                value = pinConfirm,
                                onValueChange = { pinConfirm = it },
                                label = { Text("Ingresa tu PIN") },
                                singleLine = true,
                                isError = pinConfirmError,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (pinConfirmError) {
                                Text("PIN incorrecto", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (pinConfirm == initial.pin) {
                                    // convertir foto
                                    var finalBase64 = photoBase64
                                    photoUri?.let { uri ->
                                        context.contentResolver.openInputStream(uri)
                                            ?.readBytes()?.let { Base64.encodeToString(it, Base64.DEFAULT) }
                                            ?.also { finalBase64 = it }
                                    }
                                    showPinDialog = false
                                    onSave(
                                        initial.copy(
                                            email       = email.trim(),
                                            nombre      = nombre.trim(),
                                            apellido    = apellido.trim(),
                                            celular     = celular.trim(),
                                            tipo        = tipoUsuario,
                                            sexo        = sexo,
                                            photoBase64 = finalBase64,
                                            tiendaId    = initial.tiendaId
                                        )
                                    )
                                } else {
                                    pinConfirmError = true
                                }
                            }) {
                                Text("Confirmar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showPinDialog = false
                                pinConfirm = ""
                                pinConfirmError = false
                            }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

// DrawerItem helper
@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label    = { Text(label) },
        icon     = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick  = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

// Mapea Usuario a Map
fun Usuario.toMap() = mapOf(
    "email"       to email,
    "dni"         to dni,
    "nombre"      to nombre,
    "apellido"    to apellido,
    "celular"     to celular,
    "tipo"        to tipo,
    "sexo"        to sexo,
    "pin"         to pin,
    "photoBase64" to photoBase64,
    "tiendaId"    to tiendaId
)
