package com.example.fastped

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fastped.model.Restaurante
import com.example.fastped.util.base64ToImageBitmap
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val db = Firebase.firestore

    var query by remember { mutableStateOf("") }
    var stores by remember { mutableStateOf<List<Restaurante>>(emptyList()) }

    // Listen for changes in "stores" collection
    LaunchedEffect(Unit) {
        db.collection("stores")
            .addSnapshotListener { snap, _ ->
                stores = snap?.documents
                    ?.mapNotNull { d ->
                        val name = d.getString("Nombre") ?: return@mapNotNull null
                        val desc = d.getString("Descripcion") ?: ""
                        Restaurante(
                            IDRes           = d.id,
                            CodigoUnico     = d.getString("CodigoUnico"),
                            Nombre          = name,
                            Descripcion     = desc,
                            Direccion       = d.getString("Direccion").orEmpty(),
                            Distrito        = d.getString("Distrito").orEmpty(),
                            Provincia       = d.getString("Provincia").orEmpty(),
                            BannerBase64    = d.getString("BannerBase64"),
                            StoreLogoBase64 = d.getString("StoreLogoBase64")
                        )
                    } ?: emptyList()
            }
    }

    val filtered = remember(stores, query) {
        if (query.isBlank()) stores
        else stores.filter {
            it.Nombre.contains(query.trim(), ignoreCase = true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("Buscar tiendas...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Tiendas Populares
            item {
                Text("Tiendas Populares", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val popular = stores.take(5)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(popular) { store ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    // Navegación a detalle público de tienda
                                    nav.navigate("storeDetail/${store.IDRes}")
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Box(
                                    Modifier
                                        .height(80.dp)
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    store.StoreLogoBase64?.let { b64 ->
                                        base64ToImageBitmap(b64)?.let { bmp ->
                                            Image(
                                                bitmap = bmp,
                                                contentDescription = store.Nombre,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(store.Nombre, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Lista completa de tiendas
            items(filtered) { store ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("storeDetail/${store.IDRes}") },
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            store.BannerBase64?.let { b64 ->
                                base64ToImageBitmap(b64)?.let { bmp ->
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = store.Nombre,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(store.Nombre, style = MaterialTheme.typography.titleSmall)
                            Text(store.Descripcion, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${store.Direccion}, ${store.Distrito}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
