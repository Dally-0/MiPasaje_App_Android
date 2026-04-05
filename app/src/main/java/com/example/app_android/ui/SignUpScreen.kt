package com.example.app_android.ui

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_android.data.RegisterRequest
import com.example.app_android.data.RetrofitClient
import com.example.app_android.data.RoleResponse
import kotlinx.coroutines.launch
import java.util.*
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpRequest: (RegisterRequest) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correoElectronico by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("2000-01-01") }
    var carnetIdentidad by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmContrasena by remember { mutableStateOf("") }
    
    // Roles dinámicos
    var roles by remember { mutableStateOf<List<RoleResponse>>(emptyList()) }
    var selectedRole by remember { mutableStateOf<RoleResponse?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var isLoadingRoles by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Cargar roles al inicio
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getRoles()
            if (response.isSuccessful && response.body() != null) {
                roles = response.body()!!.data
                // Seleccionar Estudiante Primaria (ID 2) por defecto si existe
                selectedRole = roles.find { it.id == 2 } ?: roles.firstOrNull()
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                Toast.makeText(context, "Error API (${response.code()}): $errorBody", Toast.LENGTH_LONG).show()
                Log.e("ROLES_ERROR", "Error API: $errorBody")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error de Red: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ROLES_ERROR", "Error al cargar roles", e)
        } finally {
            isLoadingRoles = false
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            fechaNacimiento = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(modifier = modifier.fillMaxSize()) {
        IconButton(
            onClick = { exitProcess(0) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Salir de la aplicación",
                tint = Color(0xFF4A6572)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(text = "Registro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
            Spacer(modifier = Modifier.height(24.dp))

            // Selector de Rol (SelectBox / Dropdown)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(text = "¿Cómo te registras?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoadingRoles) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = selectedRole?.nombre ?: "Seleccione un rol",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFE8E4ED),
                                unfocusedContainerColor = Color(0xFFE8E4ED)
                            ),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(text = role.nombre) },
                                    onClick = {
                                        selectedRole = role
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SignUpField(label = "Nombres", value = nombres, onValueChange = { nombres = it })
            SignUpField(label = "Apellidos", value = apellidos, onValueChange = { apellidos = it })
            SignUpField(label = "Correo Electronico", value = correoElectronico, onValueChange = { correoElectronico = it })
            
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(text = "Fecha de nacimiento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = fechaNacimiento, fontSize = 14.sp)
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            SignUpField(label = "Carnet de identidad", value = carnetIdentidad, onValueChange = { carnetIdentidad = it })
            SignUpField(label = "Contraseña", value = contrasena, onValueChange = { contrasena = it }, isPassword = true)
            SignUpField(label = "Repita contraseña", value = confirmContrasena, onValueChange = { confirmContrasena = it }, isPassword = true)

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { 
                    if (contrasena == confirmContrasena && contrasena.length >= 6 && nombres.isNotBlank() && selectedRole != null) {
                        onSignUpRequest(
                            RegisterRequest(
                                nombres = nombres,
                                apellidos = apellidos,
                                carnetIdentidad = carnetIdentidad,
                                correoElectronico = correoElectronico,
                                fechaNacimiento = fechaNacimiento,
                                contrasena = contrasena,
                                rolId = selectedRole!!.id
                            )
                        )
                    }
                },
                modifier = Modifier.width(150.dp).height(50.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
            ) {
                Text("Registrarse")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.width(150.dp).height(50.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color.Black)
            ) {
                Text("Ir al Login")
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SignUpField(label: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFE8E4ED), unfocusedContainerColor = Color(0xFFE8E4ED)),
            singleLine = true
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(onSignUpRequest = {}, onNavigateToLogin = {})
}
