// BottomBar.kt
package com.example.fastped

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.vector.ImageVector

private data class BottomNavItem(
    val routeKey: String,      // clave que usamos internamente para comparar
    val icon: ImageVector      // el ícono
)

@Composable
fun BottomBar(
    nav: NavHostController,
    currentDni: String?
) {
    // definimos solo la "clave" de la ruta, sin parámetros
    val items = listOf(
        BottomNavItem("home",        Icons.Default.Home),
        BottomNavItem("cart",        Icons.Default.ShoppingCart),
        BottomNavItem("notif",       Icons.Default.Notifications),
    )

    // obtenemos la ruta actual, por ejemplo "home" o "profile/123456"
    val currentRoute = nav.currentDestination?.route

    NavigationBar {
        items.forEach { item ->
            // seleccionamos si la ruta actual coincide:
            // para "profile" miramos si empieza con "profile/"
            val selected = if (item.routeKey == "profile") {
                currentRoute?.startsWith("profile/") == true
            } else {
                currentRoute == item.routeKey
            }

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.routeKey) },
                selected = selected,
                onClick = {
                    when (item.routeKey) {
                        "profile" -> {
                            // solo si tenemos dni navegamos
                            currentDni?.let { dni ->
                                nav.navigate("profile/$dni") {
                                    launchSingleTop = true
                                    restoreState    = true
                                    popUpTo("home") { saveState = true }
                                }
                            }
                        }
                        else -> {
                            nav.navigate(item.routeKey) {
                                launchSingleTop = true
                                restoreState    = true
                                popUpTo("home") { saveState = true }
                            }
                        }
                    }
                }
            )
        }
    }
}
