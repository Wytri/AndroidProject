// app/src/main/java/com/example/fastped/PaymentScreen.kt
package com.example.fastped

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fastped.model.EstadosPedidoProducto
import com.example.fastped.model.Pedido
import com.example.fastped.model.PedidoProducto
import com.example.fastped.util.StripeApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavHostController,
    currentDni: String,
    storeId: String,
    amount: Int // en céntimos (p.ej. 1250 = S/12.50)
) {
    val context = LocalContext.current
    val db      = Firebase.firestore
    val scope   = rememberCoroutineScope()

    // Launcher de Stripe PaymentSheet
    val paymentLauncher = rememberLauncherForActivityResult(
        contract = PaymentSheetContract(),
        onResult = { result: PaymentSheetResult ->
            when (result) {
                is PaymentSheetResult.Completed -> {
                    Toast.makeText(context, "Pago exitoso 🎉", Toast.LENGTH_LONG).show()
                    // 1) Crear el pedido en Firestore y limpiar carrito
                    scope.launch {
                        try {
                            // a) Traer sólo los ítems de esta tienda
                            val cartSnap = db.collection("users")
                                .document(currentDni)
                                .collection("cart")
                                .whereEqualTo("IDRes", storeId)
                                .get()
                                .await()

                            // b) Nuevo documento de pedido
                            val pedidoRef = db.collection("orders").document()
                            val pedidoId  = pedidoRef.id
                            // c) Calcula total real (en unidades)
                            val lista = cartSnap.documents.mapNotNull { d ->
                                d.toObject(PedidoProducto::class.java)
                                    ?.copy(IDPedido = pedidoId, Estado = EstadosPedidoProducto.RECIBIDO)
                            }
                            val totalReal = lista.sumOf { p ->
                                val desc = p.Descuento ?: 0.0
                                val precio = p.PrecioUnitario * (1 - desc/100.0)
                                precio * p.Cantidad
                            }

                            // d) Grabar cabecera
                            val pedido = Pedido(
                                idPedido    = pedidoId,
                                idCliente   = currentDni,
                                medioDePago = "Tarjeta",
                                fechaCompra = Timestamp.now(),
                                estado      = "Pagado",
                                total       = totalReal
                            )
                            pedidoRef.set(pedido).await()

                            // e) Grabar subcolección productos
                            lista.forEach { prod ->
                                pedidoRef.collection("productos")
                                    .document(prod.IDProducto)
                                    .set(prod)
                                    .await()
                            }

                            // f) Borrar ítems del carrito
                            cartSnap.documents.forEach { it.reference.delete() }

                            // g) Navegar al historial
                            navController.navigate("orderHistory") {
                                popUpTo("home")
                            }

                        } catch (e: Exception) {
                            Toast.makeText(context, "Error guardando pedido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is PaymentSheetResult.Canceled -> {
                    Toast.makeText(context, "Pago cancelado", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                is PaymentSheetResult.Failed -> {
                    Toast.makeText(
                        context,
                        "Error: ${result.error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    )

    // -- el resto de tu código de Stripe --
    var clientSecret by remember { mutableStateOf<String?>(null) }
    var loading      by remember { mutableStateOf(true) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(amount) {
        StripeApi.createPaymentIntent(amount = amount, currency = "pen") { r ->
            r.fold(
                onSuccess = { clientSecret = it },
                onFailure = { errorMsg = it.localizedMessage }
            )
            loading = false
        }
    }

    LaunchedEffect(clientSecret) {
        clientSecret?.let { secret ->
            val args = PaymentSheetContract.Args.createPaymentIntentArgs(
                secret,
                PaymentSheet.Configuration(
                    merchantDisplayName = "FastPed",
                    googlePay = PaymentSheet.GooglePayConfiguration(
                        environment   = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode   = "PE",
                        currencyCode  = "PEN"
                    )
                )
            )
            paymentLauncher.launch(args)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pagar S/%.2f".format(amount / 100.0)) }
            )
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading      -> CircularProgressIndicator()
                errorMsg != null -> Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
