// MyStoreScreen.kt
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.fastped.model.Producto
import com.example.fastped.model.Restaurante
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Solo mapea los campos que el usuario puede editar, sin tocar createdBy, createdAt, etc. */
private fun Restaurante.toUpdateMap(): Map<String, Any?> = mapOf(
    "CodigoUnico"     to CodigoUnico,
    "Nombre"          to Nombre,
    "Descripcion"     to Descripcion,
    "Direccion"       to Direccion,
    "Distrito"        to Distrito,
    "Provincia"       to Provincia,
    "BannerBase64"    to BannerBase64,
    "StoreLogoBase64" to StoreLogoBase64
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStoreScreen(
    nav: NavHostController,
    storeId: String,
    currentDni: String,
    onLogout: () -> Unit,
    onSettings: () -> Unit
) {
    val db    = Firebase.firestore
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    var store    by remember { mutableStateOf<Restaurante?>(null) }
    var products by remember { mutableStateOf<List<Producto>>(emptyList()) }

    // Campos editables
    var nombre      by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var direccion   by remember { mutableStateOf("") }
    var distrito    by remember { mutableStateOf("") }
    var provincia   by remember { mutableStateOf("") }

    // Banner / Logo
    var bannerBase64 by remember { mutableStateOf<String?>(null) }
    var logoBase64   by remember { mutableStateOf<String?>(null) }
    var bannerUri: Uri? by remember { mutableStateOf(null) }
    var logoUri: Uri?   by remember { mutableStateOf(null) }

    val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { bannerUri = it; bannerBase64 = null }
    }
    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { logoUri = it; logoBase64 = null }
    }

    LaunchedEffect(storeId) {
        // 1) Carga inicial de datos de la tienda
        val doc = db.collection("stores").document(storeId).get().await()
        if (doc.exists()) {
            Restaurante(
                IDRes           = doc.id,
                CodigoUnico     = doc.getString("CodigoUnico"),
                Nombre          = doc.getString("Nombre").orEmpty(),
                Descripcion     = doc.getString("Descripcion").orEmpty(),
                Direccion       = doc.getString("Direccion").orEmpty(),
                Distrito        = doc.getString("Distrito").orEmpty(),
                Provincia       = doc.getString("Provincia").orEmpty(),
                BannerBase64    = doc.getString("BannerBase64"),
                StoreLogoBase64 = doc.getString("StoreLogoBase64")
            ).also {
                store = it
                nombre       = it.Nombre
                descripcion  = it.Descripcion
                direccion    = it.Direccion
                distrito     = it.Distrito
                provincia    = it.Provincia
                bannerBase64 = it.BannerBase64
                logoBase64   = it.StoreLogoBase64
            }
        }

        // 2) Escuchar productos
        db.collection("stores").document(storeId)
            .collection("products")
            .addSnapshotListener { snap, _ ->
                products = snap?.documents?.map { d ->
                    Producto(
                        IDProducto      = d.id,
                        IDRes           = d.getString("IDRes").orEmpty(),
                        NombreProducto  = d.getString("NombreProducto").orEmpty(),
                        Descripcion     = d.getString("Descripcion").orEmpty(),
                        Precio          = d.getDouble("Precio") ?: 0.0,
                        Descuento       = d.getDouble("Descuento") ?: 0.0,
                        PhotoBase64     = d.getString("PhotoBase64")
                    )
                } ?: emptyList()
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
                title = { Text(store?.Nombre ?: "Mi Tienda", fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            // Prepara los valores finales de banner/logo
                            val finalBanner = bannerBase64 ?: bannerUri?.let { uri ->
                                ctx.contentResolver.openInputStream(uri)!!.readBytes()
                                    .let { Base64.encodeToString(it, Base64.DEFAULT) }
                            }
                            val finalLogo = logoBase64 ?: logoUri?.let { uri ->
                                ctx.contentResolver.openInputStream(uri)!!.readBytes()
                                    .let { Base64.encodeToString(it, Base64.DEFAULT) }
                            }
                            store?.let {
                                val updated = it.copy(
                                    Nombre          = nombre.trim(),
                                    Descripcion     = descripcion.trim(),
                                    Direccion       = direccion.trim(),
                                    Distrito        = distrito.trim(),
                                    Provincia       = provincia.trim(),
                                    BannerBase64    = finalBanner,
                                    StoreLogoBase64 = finalLogo
                                )
                                // Usa update para NO sobreescribir campos como createdBy/createdAt
                                db.collection("stores")
                                    .document(storeId)
                                    .update(updated.toUpdateMap())
                                    .await()
                                store = updated
                                Toast.makeText(ctx, "Tienda actualizada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar tienda")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("addProduct/$storeId") }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Banner y logo con scroll interno
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { pickBanner.launch("image/*") },
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when {
                            bannerUri != null -> Image(
                                painter = rememberAsyncImagePainter(bannerUri),
                                contentDescription = "Nuevo banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            !bannerBase64.isNullOrEmpty() -> base64ToImageBitmap(bannerBase64!!)?.let { bmp ->
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Banner guardado",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> Text(
                                "Toca para elegir banner",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Logo
                Card(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { pickLogo.launch("image/*") },
                    shape     = CircleShape,
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when {
                            logoUri != null -> Image(
                                painter = rememberAsyncImagePainter(logoUri),
                                contentDescription = "Nuevo logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            !logoBase64.isNullOrEmpty() -> base64ToImageBitmap(logoBase64!!)?.let { bmp ->
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Logo guardado",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> Icon(
                                Icons.Default.Storefront,
                                contentDescription = "Logo por defecto",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = distrito,
                        onValueChange = { distrito = it },
                        label = { Text("Distrito") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = provincia,
                        onValueChange = { provincia = it },
                        label = { Text("Provincia") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Productos / Platillos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Grid de productos
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.IDProducto }) { prod ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                nav.navigate("editProduct/$storeId/${prod.IDProducto}")
                            },
                        shape     = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                prod.PhotoBase64?.let { b64 ->
                                    base64ToImageBitmap(b64)?.let { bmp ->
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = prod.NombreProducto,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } ?: Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(prod.NombreProducto, fontSize = 12.sp, maxLines = 1)
                            if (prod.Descuento > 0.0) {
                                val final = prod.Precio - prod.Descuento
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "S/%.2f".format(prod.Precio),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "S/%.2f".format(final),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Text("S/%.2f".format(prod.Precio), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}