// AppNavHost.kt
package com.example.fastped

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.fastped.model.Usuario
import com.example.fastped.ui.NotificationsScreen
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

    // 1) Estado de sesión
    var currentDni  by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<Usuario?>(null) }

    // 2) Cuando cambie el DNI, recarga todo el Usuario
    LaunchedEffect(currentDni) {
        currentUser = currentDni?.let { dni ->
            val snap = db.collection("users").document(dni).get().await()
            if (!snap.exists()) return@let null

            Usuario(
                email       = snap.getString("email").orEmpty(),
                dni         = dni,
                nombre      = snap.getString("nombre").orEmpty(),
                apellido    = snap.getString("apellido").orEmpty(),
                celular     = snap.getString("celular").orEmpty(),
                pin         = snap.getString("pin").orEmpty(),
                tipo        = snap.getLong("tipo")?.toInt() ?: 0,
                sexo        = snap.getLong("sexo")?.toInt() ?: 0,
                photoBase64 = snap.getString("photoBase64"),
                tiendaId    = snap.getString("tiendaId")
            )
        }
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    @Composable
    fun DrawerContent() {
        ModalDrawerSheet {
            Text("Menú", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

            // — Mi Perfil —
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Mi Perfil") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    currentDni?.let { navController.navigate("profile/$it") }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            Divider()

            // — Historial de pedidos —
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.History, contentDescription = null) },
                label = { Text("Historial de pedidos") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("orderHistory")
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            // — Tiendas favoritas —
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                label = { Text("Tiendas favoritas") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("favoriteStores")
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            Divider()

            // — Owner (tipo == 2) —
            currentUser?.takeIf { it.tipo == 2 }?.let { user ->
                if (user.tiendaId.isNullOrEmpty()) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Store, contentDescription = null) },
                        label = { Text("Crear mi tienda") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("createStore/${user.dni}")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        label = { Text("Mi tienda") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("myStore/${user.tiendaId}")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    // ← Aquí añadimos el acceso a WorkPlace para el dueño:
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Work, contentDescription = null) },
                        label = { Text("Mi lugar de trabajo") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("workPlace/${user.tiendaId}")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Divider()
            }

            // — Trabajador (tipo == 3) —
            currentUser?.takeIf { it.tipo == 3 }?.let { user ->
                if (user.tiendaId.isNullOrEmpty()) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                        label = { Text("Registrarme en tienda") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("joinStore/${user.dni}")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                } else {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Work, contentDescription = null) },
                        label = { Text("Mi lugar de trabajo") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("workPlace/${user.tiendaId}")
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Divider()
            }

            Spacer(Modifier.weight(1f))

            // — Cerrar sesión —
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                label = { Text("Cerrar sesión") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
                    currentDni = null
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    // 3) Función única con TODO tu NavGraph original
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun MainNavHost(modifier: Modifier = Modifier) {
        NavHost(
            navController    = navController,
            startDestination = "login",
            modifier         = modifier.fillMaxSize()
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

            // Checkout
            composable(
                route = "checkout/{dni}",
                arguments = listOf(
                    navArgument("dni") { type = NavType.StringType }
                )
            ) { back ->
                val dni = back.arguments!!.getString("dni")!!
                CheckoutScreen(
                    // aquí debes usar `nav = navController`
                    nav        = navController,
                    currentDni = dni
                )
            }
            // Payment (pasa la instancia aquí)
            composable(
                route = "payment/{dni}/{storeId}/{amount}",
                arguments = listOf(
                    navArgument("dni")      { type = NavType.StringType },
                    navArgument("storeId")  { type = NavType.StringType },
                    navArgument("amount")   { type = NavType.IntType }
                )
            ) { back ->
                val dni     = back.arguments!!.getString("dni")!!
                val storeId = back.arguments!!.getString("storeId")!!
                val amount  = back.arguments!!.getInt("amount")
                // Llamada CORRECTA al PaymentScreen Compose-only:
                PaymentScreen(
                    navController = navController,
                    currentDni    = dni,
                    storeId       = storeId,
                    amount        = amount
                )
            }

            composable("notif") {
                NotificationsScreen(currentUserId = currentDni!!)
            }

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
// 4) Renderizado: con o sin Drawer según sesión
if (currentDni != null) {
    // — Sesión iniciada: Drawer + TopBar + BottomBar + NavGraph
    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = { DrawerContent() }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    title = {
                        val route = navController.currentDestination?.route
                        Text(
                            when {
                                route == "home"                       -> "Inicio"
                                route == "cart"                       -> "Carrito"
                                route == "orderHistory"               -> "Historial"
                                route?.startsWith("profile/") == true -> "Mi Perfil"
                                else                                  -> ""
                            }
                        )
                    }
                )
            },
            bottomBar = {
                val backStack    by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route
                if (currentRoute in listOf("home", "cart", "notif")) {
                    BottomBar(navController, currentDni!!)
                }
            }
        ) { innerPadding ->

            // solo extraemos la parte de padding de arriba:
            //val topOnly = innerPadding.calculateTopPadding()

            val top    = innerPadding.calculateTopPadding()
            val bottom = innerPadding.calculateBottomPadding()

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = top, bottom = bottom)
            ) {
                MainNavHost()  // tu NavHost como antes
            }
        }
    }
} else {
    // — Antes de login: sin drawer, sin topBar, sin bottomBar
    Scaffold { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            MainNavHost()
        }
    }
}
}