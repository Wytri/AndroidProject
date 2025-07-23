// NotificationsScreen.kt
package com.example.fastped.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.example.fastped.MainActivity
import com.example.fastped.model.Pedido
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class NotificationItem(
    val orderId: String,
    val message: String,
    val timestamp: LocalDateTime
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(currentUserId: String) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val zone = ZoneId.systemDefault()

    // memoria de notificaciones y últimos estados
    val notifications = remember { mutableStateListOf<NotificationItem>() }
    val lastStatus = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(currentUserId) {
        // 1) Crear canal para Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "order_updates",
                "Actualizaciones de pedidos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de cambios de estado de tus pedidos"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        // 2) Fetch inicial: todos los pedidos de este cliente
        val initialSnap = db.collection("orders")
            .whereEqualTo("idcliente", currentUserId)
            .get().await()

        for (doc in initialSnap.documents) {
            val pid = doc.id
            val estado = doc.getString("estado") ?: continue
            // Si ya cambió de "Pagado", generamos UI + local
            if (estado != "Pagado") {
                val msg = "Tu pedido $pid está \"$estado\""
                sendLocalNotification(context, pid.hashCode(), msg)
                notifications += NotificationItem(
                    orderId = pid,
                    message = msg,
                    timestamp = LocalDateTime.ofInstant(Instant.now(), zone)
                )
            }
            // Guardamos el estado actual
            lastStatus[pid] = estado
        }

        // 3) Luego enganchar listener para futuros cambios
        db.collection("orders")
            .whereEqualTo("idcliente", currentUserId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener

                for (dc in snap.documentChanges) {
                    val pid = dc.document.id
                    val pedido = dc.document.toObject(Pedido::class.java)
                    val newState = pedido.estado
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            // ya lo inicializamos arriba
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val oldState = lastStatus[pid]
                            if (oldState != null && oldState != newState) {
                                val msg = "Tu pedido $pid cambió a \"$newState\""
                                sendLocalNotification(context, pid.hashCode(), msg)
                                notifications += NotificationItem(
                                    orderId = pid,
                                    message = msg,
                                    timestamp = LocalDateTime.ofInstant(Instant.now(), zone)
                                )
                            }
                        }
                        else -> {}
                    }
                    lastStatus[pid] = newState
                }
            }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("Notificaciones") })
    }) { padding ->
        if (notifications.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay notificaciones aún")
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(notifications) { note ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(text = note.message, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = note.timestamp
                                    .toString()
                                    .replace("T", " "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sendLocalNotification(context: Context, id: Int, message: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, "order_updates")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Pedido actualizado")
        .setContentText(message)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(id, notification)
}
