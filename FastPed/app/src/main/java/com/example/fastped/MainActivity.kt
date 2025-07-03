package com.example.fastped

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fastped.ui.theme.FastPedTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializa Firebase
        FirebaseApp.initializeApp(this)

        // 2. Ajusta para Edge-to-Edge y establece el contenido Compose
        enableEdgeToEdge()
        setContent {
            FastPedTheme {
                // 3. Lanza tu único NavHost global
                AppNavHost()
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FastPedTheme {
        Greeting("Android")
    }
}
