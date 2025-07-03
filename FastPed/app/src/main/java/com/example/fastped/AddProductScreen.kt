// AddProductScreen.kt
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.fastped.model.Producto
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    nav: NavHostController,
    storeId: String,
    productId: String?       // null = crear, no-null = editar
) {
    val db    = Firebase.firestore
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    // Formularios
    var name    by remember { mutableStateOf("") }
    var desc    by remember { mutableStateOf("") }
    var price   by remember { mutableStateOf("") }
    var hasDisc by remember { mutableStateOf(false) }
    var disc    by remember { mutableStateOf("") }

    // Foto
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var photoUri: Uri? by remember { mutableStateOf(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { photoUri = it; photoBase64 = null }
    }

    // Si estamos editando, cargamos los datos
    LaunchedEffect(productId) {
        productId?.let { id ->
            val doc = db.collection("stores")
                .document(storeId)
                .collection("products")
                .document(id)
                .get()
                .await()
            if (doc.exists()) {
                name = doc.getString("NombreProducto").orEmpty()
                desc = doc.getString("Descripcion").orEmpty()
                price = doc.getDouble("Precio")?.toString() ?: ""
                val d = doc.getDouble("Descuento") ?: 0.0
                hasDisc = d > 0.0
                disc = if (hasDisc) d.toString() else ""
                photoBase64 = doc.getString("PhotoBase64")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text(if (productId == null) "Nuevo Producto" else "Editar Producto") },
                actions = {
                    IconButton(onClick = {
                        // Validación
                        val p = price.toDoubleOrNull()
                        val d = disc.toDoubleOrNull() ?: 0.0
                        if (name.isBlank() || p == null || (hasDisc && disc.isBlank())) {
                            Toast.makeText(ctx, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        scope.launch {
                            // Prepara foto
                            var finalPhoto = photoBase64
                            photoUri?.let { uri ->
                                ctx.contentResolver.openInputStream(uri)
                                    ?.readBytes()
                                    ?.let { bytes ->
                                        finalPhoto = Base64.encodeToString(bytes, Base64.DEFAULT)
                                    }
                            }
                            // Referencia Firestore
                            val col = db.collection("stores")
                                .document(storeId)
                                .collection("products")
                            val docRef = if (productId == null) col.document() else col.document(productId)
                            val id = docRef.id

                            val producto = Producto(
                                IDProducto     = id,
                                IDRes          = storeId,
                                NombreProducto = name.trim(),
                                Descripcion    = desc.trim(),
                                Precio         = p,
                                Descuento      = if (hasDisc) d else 0.0,
                                PhotoBase64    = finalPhoto
                            )
                            // Guardar
                            docRef.set(producto.toMap()).await()
                            Toast.makeText(
                                ctx,
                                if (productId == null) "Producto creado" else "Producto actualizado",
                                Toast.LENGTH_SHORT
                            ).show()
                            nav.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
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
            // Selector de foto
            Box(
                Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { pickPhoto.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    photoUri != null ->
                        Image(
                            painter = rememberAsyncImagePainter(photoUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    !photoBase64.isNullOrEmpty() -> {
                        base64ToImageBitmap(photoBase64!!)?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                    else -> Text("Tocar para foto", fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Precio (ej. 12.50)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDisc, onCheckedChange = { hasDisc = it })
                Text("¿Aplicar descuento?")
            }
            if (hasDisc) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = disc,
                    onValueChange = { disc = it },
                    label = { Text("Descuento (ej. 2.00)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
