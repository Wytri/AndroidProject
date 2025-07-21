// app/src/main/java/com/example/fastped/ProfileScreen.kt
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    nav: NavHostController,
    initial: Usuario,
    onSave: (Usuario) -> Unit,
    onLogout: () -> Unit
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()

    // Form state
    var email       by remember { mutableStateOf(initial.email) }
    var nombre      by remember { mutableStateOf(initial.nombre) }
    var apellido    by remember { mutableStateOf(initial.apellido) }
    var celular     by remember { mutableStateOf(initial.celular) }
    var tipoUsuario by remember { mutableStateOf(initial.tipo) }
    var sexo        by remember { mutableStateOf(initial.sexo) }
    var tipoError   by remember { mutableStateOf(false) }
    var sexoError   by remember { mutableStateOf(false) }

    // Photo state
    var photoBase64 by remember { mutableStateOf(initial.photoBase64) }
    var photoUri: Uri? by remember { mutableStateOf(null) }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            photoBase64 = null
        }
    }

    // PIN confirmation dialog
    var showPinDialog   by remember { mutableStateOf(false) }
    var pinConfirm      by remember { mutableStateOf("") }
    var pinConfirmError by remember { mutableStateOf(false) }

    val tiposList = listOf("Elige tipo", "Cliente", "Dueño de tienda", "Trabajador")
    val sexoList  = listOf("Elige sexo", "Hombre", "Mujer", "Prefiero no decir")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
            // Avatar picker
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
                        base64ToImageBitmap(photoBase64!!)?.let { bmp ->
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

            Text("DNI/Passport/CE: ${initial.dni}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

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

            // Tipo de usuario dropdown
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

            // Sexo dropdown
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
        }
    }

    // PIN Confirmation Dialog
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
                        var finalBase64 = photoBase64
                        photoUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)
                                ?.readBytes()
                                ?.let { Base64.encodeToString(it, Base64.DEFAULT) }
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
