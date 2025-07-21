package com.example.fastped

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(
    email: String,
    dni: String,
    onDone: (
        nombre: String,
        apellido: String,
        celular: String,
        pin: String,
        tipoUsuario: Int,
        sexo: Int
    ) -> Unit
) {
    var nombre     by remember { mutableStateOf("") }
    var apellido   by remember { mutableStateOf("") }
    var celular    by remember { mutableStateOf("") }
    var pin        by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }

    // — Listas y estado para dropdowns —
    val tiposList = listOf("Elige tipo", "Cliente", "Dueño de tienda", "Trabajador")
    var tipoIndex   by remember { mutableStateOf(0) }
    var expandedTipo by remember { mutableStateOf(false) }
    var tipoError   by remember { mutableStateOf(false) }

    val sexoList = listOf("Elige sexo", "Hombre", "Mujer", "Prefiero no decir")
    var sexoIndex   by remember { mutableStateOf(0) }
    var expandedSexo by remember { mutableStateOf(false) }
    var sexoError   by remember { mutableStateOf(false) }

    var pinError    by remember { mutableStateOf(false) }
    var matchError  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),  // <— hacemos scroll
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Completa tus datos", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Correo y DNI (solo lectura)
        OutlinedTextField(
            value = email, onValueChange = {},
            label = { Text("Correo electrónico") },
            enabled = false, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = dni, onValueChange = {},
            label = { Text("DNI/Passport/CE") },
            enabled = false, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Nombre / Apellido / Celular
        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            label = { Text("Nombre") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apellido, onValueChange = { apellido = it },
            label = { Text("Apellido") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = celular, onValueChange = { celular = it },
            label = { Text("Celular") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Dropdown: Tipo de usuario
        ExposedDropdownMenuBox(
            expanded = expandedTipo,
            onExpandedChange = { expandedTipo = !expandedTipo }
        ) {
            OutlinedTextField(
                value = tiposList[tipoIndex],
                onValueChange = { },
                readOnly = true,
                isError = tipoError,
                label = { Text("Tipo de usuario") },
                trailingIcon = { TrailingIcon(expandedTipo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()  // <- aquí el anclaje
            )
            ExposedDropdownMenu(
                expanded = expandedTipo,
                onDismissRequest = { expandedTipo = false }
            ) {
                tiposList.forEachIndexed { idx, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            tipoIndex = idx
                            tipoError = idx == 0
                            expandedTipo = false
                        }
                    )
                }
            }
        }
        if (tipoError) {
            Text("Debes elegir un tipo", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))

        // Dropdown: Sexo
        ExposedDropdownMenuBox(
            expanded = expandedSexo,
            onExpandedChange = { expandedSexo = !expandedSexo }
        ) {
            OutlinedTextField(
                value = sexoList[sexoIndex],
                onValueChange = { },
                readOnly = true,
                isError = sexoError,
                label = { Text("Sexo") },
                trailingIcon = { TrailingIcon(expandedSexo) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()  // <- y aquí
            )
            ExposedDropdownMenu(
                expanded = expandedSexo,
                onDismissRequest = { expandedSexo = false }
            ) {
                sexoList.forEachIndexed { idx, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            sexoIndex = idx
                            sexoError = idx == 0
                            expandedSexo = false
                        }
                    )
                }
            }
        }
        if (sexoError) {
            Text("Debes elegir un sexo", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))

        // PIN + confirmación
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 6) pin = it
            },
            isError = pinError,
            label = { Text("PIN (6 dígitos)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        if (pinError) {
            Text("El PIN debe tener 6 dígitos", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pinConfirm,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 6) pinConfirm = it
            },
            isError = matchError,
            label = { Text("Repite el PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        if (matchError) {
            Text("Los PIN no coinciden", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))

        // Botón Guardar
        Button(
            onClick = {
                // validaciones
                pinError   = pin.length != 6
                matchError = pin != pinConfirm
                tipoError  = tipoIndex == 0
                sexoError  = sexoIndex == 0

                if (!pinError && !matchError && !tipoError && !sexoError) {
                    onDone(
                        nombre.trim(),
                        apellido.trim(),
                        celular.trim(),
                        pin.trim(),
                        tipoIndex,
                        sexoIndex
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("GUARDAR USUARIO")
        }
        }
    }
}
