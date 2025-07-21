// RegistrationScreen.kt
package com.example.fastped

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(onSubmit: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var dni   by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf(false) }
    var dniError   by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Creación de Usuario", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Introduce tu email y tu DNI", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            isError = emailError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (emailError) {
            Text(
                "Debe ingresar un correo válido",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }
        Spacer(Modifier.height(12.dp))

        // DNI
        OutlinedTextField(
            value = dni,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 8) dni = it
            },
            label = { Text("DNI/Passport/CE") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            isError = dniError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        if (dniError) {
            Text(
                "Debe ingresar su DNI/Passport/CE",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Start)
            )
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                emailError = email.isBlank() || !email.contains("@")
                dniError   = dni.isBlank()
                if (!emailError && !dniError) {
                    onSubmit(email.trim(), dni.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("CONTINUAR")
        }
    }
}
