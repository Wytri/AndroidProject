package com.example.fastped

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fastped.ui.theme.FastPedTheme
import com.google.firebase.FirebaseApp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializa Firebase
        FirebaseApp.initializeApp(this)
        PaymentConfiguration.init(
            this,
            "pk_test_51RnEgGPW2JlMPOP0qv8tkhZz88vETzpu7sW3R73E65iPK5CZ74EwcYPEjeyTx23YHAcVpAd3u56Ctqidb5IOfBW000FsbTbnZD"
        )

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
