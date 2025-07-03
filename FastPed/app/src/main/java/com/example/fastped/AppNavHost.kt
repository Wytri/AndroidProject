// AppNavHost.kt
package com.example.fastped

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.fastped.model.Usuario
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val db            = Firebase.firestore
    val ctx           = LocalContext.current
    val scope         = rememberCoroutineScope()

    var currentDni by remember { mutableStateOf<String?>(null) }

    val backStack    by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = when {
        currentRoute == "home"                       -> true
        currentRoute == "cart"                       -> true
        currentRoute == "notif"                      -> true
        currentRoute?.startsWith("profile/") == true -> currentDni != null
        else                                         -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar && currentDni != null) {
                BottomBar(navController, currentDni!!)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "login",
            modifier         = Modifier.padding(innerPadding)
        ) {
            // LOGIN
            composable("login") {
                LoginScreen(
                    onLogin = { dni, pin ->
                        db.collection("users").document(dni)
                            .get()
                            .addOnSuccessListener { doc ->
                                val storedPin = doc.getString("pin")
                                if (doc.exists() && storedPin == pin) {
                                    currentDni = dni
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(ctx, "DNI o PIN incorrectos", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(ctx, "Error de red", Toast.LENGTH_SHORT).show()
                            }
                    },
                    onCreateAccount = {
                        navController.navigate("register")
                    }
                )
            }

            // REGISTER
            composable("register") {
                RegistrationScreen { email, dni ->
                    db.collection("users").document(dni)
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                Toast.makeText(ctx, "DNI ya registrado", Toast.LENGTH_LONG).show()
                            } else {
                                db.collection("users")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener { query ->
                                        if (!query.isEmpty) {
                                            Toast.makeText(ctx, "Correo ya en uso", Toast.LENGTH_LONG).show()
                                        } else {
                                            navController.navigate("details/$email/$dni")
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(ctx, "Error al verificar correo", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(ctx, "Error de red", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            // DETAILS
            composable(
                "details/{email}/{dni}",
                arguments = listOf(
                    navArgument("email") { type = NavType.StringType },
                    navArgument("dni")   { type = NavType.StringType }
                )
            ) { back ->
                val email = back.arguments!!.getString("email")!!
                val dni   = back.arguments!!.getString("dni")!!
                ProfileDetailsScreen(email = email, dni = dni) { nombre, apellido, celular, pin, tipo, sexo ->
                    val userData = mapOf(
                        "email"       to email,
                        "dni"         to dni,
                        "nombre"      to nombre,
                        "apellido"    to apellido,
                        "celular"     to celular,
                        "pin"         to pin,
                        "tipo"        to tipo,
                        "sexo"        to sexo,
                        "photoBase64" to null,
                        "tiendaId"    to null
                    )
                    db.collection("users").document(dni)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(ctx, "Usuario creado 🎉", Toast.LENGTH_SHORT).show()
                            currentDni = dni
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(ctx, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            }

            // HOME
            composable("home") {
                HomeScreen(navController)
            }

            // CART & NOTIF
            composable("cart") {
                CartScreen(navController, currentDni!!)
            }
            composable("notif") { /* TODO */ }

            // PROFILE
            composable(
                "profile/{dni}",
                arguments = listOf(navArgument("dni") { type = NavType.StringType })
            ) { back ->
                val dniArg = back.arguments!!.getString("dni")!!
                var usuario by remember { mutableStateOf<Usuario?>(null) }

                LaunchedEffect(dniArg) {
                    val doc = db.collection("users").document(dniArg).get().await()
                    if (doc.exists()) {
                        usuario = Usuario(
                            email       = doc.getString("email").orEmpty(),
                            dni         = dniArg,
                            nombre      = doc.getString("nombre").orEmpty(),
                            apellido    = doc.getString("apellido").orEmpty(),
                            celular     = doc.getString("celular").orEmpty(),
                            tipo        = doc.getLong("tipo")?.toInt() ?: 0,
                            sexo        = doc.getLong("sexo")?.toInt() ?: 0,
                            pin         = doc.getString("pin").orEmpty(),
                            photoBase64 = doc.getString("photoBase64"),
                            tiendaId    = doc.getString("tiendaId")
                        )
                    }
                }

                if (usuario == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ProfileScreen(
                        nav     = navController,
                        initial = usuario!!,
                        onSave  = { updated ->
                            scope.launch {
                                try {
                                    db.collection("users")
                                        .document(updated.dni)
                                        .set(updated.toMap())
                                        .await()
                                    Toast.makeText(ctx, "Cambios guardados", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onLogout = {
                            currentDni = null
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }

            // CREATE STORE
            composable(
                "createStore/{dni}",
                arguments = listOf(navArgument("dni") { type = NavType.StringType })
            ) { back ->
                val ownerDni = back.arguments!!.getString("dni")!!
                CreateStoreScreen(navController, ownerDni)
            }

            // MY STORE
            composable(
                "myStore/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { back ->
                val storeId = back.arguments!!.getString("storeId")!!
                MyStoreScreen(
                    nav        = navController,
                    storeId    = storeId,
                    currentDni = currentDni!!,
                    onLogout   = {
                        currentDni = null
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onSettings = { navController.navigate("storeSettings/$storeId") }
                )
            }

            // ADD NEW PRODUCT
            composable(
                "addProduct/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { back ->
                val storeId = back.arguments!!.getString("storeId")!!
                AddProductScreen(
                    nav       = navController,
                    storeId   = storeId,
                    productId = null
                )
            }

            // EDIT EXISTING PRODUCT
            composable(
                "editProduct/{storeId}/{productId}",
                arguments = listOf(
                    navArgument("storeId")  { type = NavType.StringType },
                    navArgument("productId"){ type = NavType.StringType }
                )
            ) { back ->
                val storeId   = back.arguments!!.getString("storeId")!!
                val productId = back.arguments!!.getString("productId")!!
                AddProductScreen(
                    nav       = navController,
                    storeId   = storeId,
                    productId = productId
                )
            }

            // STORE SETTINGS
            composable(
                "storeSettings/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { back ->
                val storeId   = back.arguments!!.getString("storeId")!!
                val dniLogged = currentDni ?: error("Debe haber un usuario logueado")
                StoreSettingsScreen(navController, storeId, dniLogged) {
                    navController.popBackStack()
                }
            }
            // --- JOIN STORE (trabajador) ---
            composable(
                "joinStore/{dni}",
                arguments = listOf(navArgument("dni") { type = NavType.StringType })
            ) { back ->
                val dni = back.arguments!!.getString("dni")!!
                JoinStoreScreen(navController, dni)
            }

            // Agrega ruta pública de detalle de tienda
            composable(
                "storeDetail/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { back ->
                val storeId = back.arguments!!.getString("storeId")!!
                if (currentDni.isNullOrBlank()) {
                    // Si no hay usuario logueado, regresamos a login
                    LaunchedEffect(Unit) {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    StoreDetailScreen(
                        nav        = navController,
                        storeId    = storeId,
                        currentDni = currentDni!!
                    )
                }
            }
            // HISTORIAL DE PEDIDOS (usuario)
            composable("orderHistory") {
                val currentDniNav = currentDni ?: ""
                if (currentDniNav.isNotBlank()) {
                    OrderHistoryScreen(
                        currentDni = currentDniNav,
                        nav = navController
                    )
                } else {
                    // Redirige a login si por alguna razón no hay usuario
                    LaunchedEffect(Unit) {
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            // DETALLE DEL PEDIDO
            composable(
                "orderDetail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { back ->
                val orderId = back.arguments!!.getString("orderId")!!
                OrderDetailScreen(orderId = orderId, nav = navController)
            }
            // --- WORK PLACE (trabajador con tienda) ---
            composable(
                "workPlace/{storeId}",
                arguments = listOf(navArgument("storeId") { type = NavType.StringType })
            ) { back ->
                val storeId = back.arguments!!.getString("storeId")!!

                // Asegúrate de que currentDni no sea null aquí
                val dni = currentDni
                    ?: error("No hay usuario logueado para entrar a WorkPlace")

                WorkPlaceScreen(
                    nav     = navController,
                    storeId = storeId,
                    userDni = dni
                )
            }
        }
    }
}
