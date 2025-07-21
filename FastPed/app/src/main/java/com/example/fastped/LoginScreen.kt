// LoginScreen.kt
package com.example.fastped

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onCreateAccount: () -> Unit
) {
    var dni by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    // Validación local
    var dniError by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }

    val azulClaro = Color(0xFF005EB8)
    val azulOscuro = Color(0xFF003F7D)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(azulClaro, azulOscuro))
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Login de Usuario",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Inicia sesión en tu cuenta",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Spacer(Modifier.height(24.dp))

        // Campo DNI
        OutlinedTextField(
            value = dni,
            onValueChange = {
                if (it.length <= 8 && it.all(Char::isDigit)) {
                    dni = it
                }
            },
            label = { Text("DNI/Passport/CE") },
            singleLine = true,
            isError = dniError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedTextFieldColors(
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )
        if (dniError) {
            Text(
                "Debes ingresar tu DNI",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp, top = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))

        // Campo PIN
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 6 && it.all(Char::isDigit)) {
                    pin = it
                }
            },
            label = { Text("PIN (6 dígitos)") },
            singleLine = true,
            isError = pinError,
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
            },
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        imageVector = if (showPin) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            },
            visualTransformation = if (showPin)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedTextFieldColors(
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )
        if (pinError) {
            Text(
                "Debes ingresar tu PIN",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp, top = 2.dp)
            )
        }
        Spacer(Modifier.height(24.dp))

        // Botón Continue
        Button(
            onClick = {
                dniError = dni.isBlank()
                pinError = pin.isBlank()
                if (!dniError && !pinError) {
                    onLogin(dni.trim(), pin.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = azulClaro
            )
        ) {
            Text("Continue")
        }
        Spacer(Modifier.height(8.dp))

        // Botón Crear usuario
        Button(
            onClick = onCreateAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = azulOscuro,
                contentColor = Color.White
            )
        ) {
            Text("Crear usuario")
        }
        Spacer(Modifier.height(16.dp))

        // Separator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.6f))
            Text("  or  ", color = Color.White.copy(alpha = 0.8f))
            Divider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(16.dp))

        // Social buttons
        OutlinedButton(
            onClick = { /* TODO: Google */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Continue with Google")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { /* TODO: Apple */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
        ) {
            Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Continue with Apple")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "By clicking continue, you agree to our Terms of Service and Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
