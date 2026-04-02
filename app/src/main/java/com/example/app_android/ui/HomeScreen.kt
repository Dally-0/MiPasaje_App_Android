package com.example.app_android.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

@Composable
fun HomeScreen(
    userName: String = "NombreUsuario",
    fullNames: String = "Nombres y apellidos",
    email: String = "Correo electronico",
    rolId: Int = 2, // 2: Estudiante, 3: Chofer (según el Seeder)
    saldo: String = "0.00",
    onActionClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Según tu seeder: 1: Admin, 2: Estudiante, 3: Chofer
    val isChofer = rolId == 3

    Box(modifier = modifier.fillMaxSize()) {
        // Fila de botones en la esquina superior derecha
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar saldo",
                    tint = Color(0xFF4A6572)
                )
            }
            IconButton(onClick = { exitProcess(0) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Salir de la aplicación",
                    tint = Color(0xFF4A6572)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Hola $userName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A6572)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // User Info Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Cuenta:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
                Text(text = fullNames, fontSize = 14.sp, color = Color(0xFF4A6572))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Correo:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
                Text(text = email, fontSize = 14.sp, color = Color(0xFF4A6572))
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Balance dinámico según el rol
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isChofer) "Ganancias hoy:" else "Saldo disponible:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A6572)
                )
                Text(text = "$saldo BS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Transactions Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = if (isChofer) "Últimos cobros:" else "Lista de transacciones:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A6572)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                    
                    TransactionItem(isChofer)
                    
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                    
                    TransactionItem(isChofer)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de acción dinámico
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isChofer) Color(0xFF2E7D32) else Color(0xFF424242),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isChofer) "COBRAR PASAJE" else "PAGAR PASAJE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun TransactionItem(isChofer: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isChofer) "Cobro realizado:" else "Pago realizado:",
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold
            )
            Text(text = "1.50 BS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "05 - 10 - 2024 - 08:30 AM",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreviewEstudiante() {
    HomeScreen(rolId = 2)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreviewChofer() {
    HomeScreen(rolId = 3)
}
