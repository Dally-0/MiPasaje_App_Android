package com.example.app_android.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_android.data.TransactionResponse
import kotlin.system.exitProcess

@Composable
fun HomeScreen(
    userName: String = "Usuario",
    fullNames: String = "Nombres y apellidos",
    email: String = "Correo electronico",
    rolId: Int = 2,
    saldo: String = "0.00",
    transactions: List<TransactionResponse> = emptyList(),
    onActionClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChofer = rolId == 3

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                    tint = Color(0xFF4A6572)
                )
            }
            IconButton(onClick = { exitProcess(0) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Salir",
                    tint = Color(0xFF4A6572)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Hola $userName",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A6572)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // User Info
            InfoRow(label = "Cuenta:", value = fullNames)
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = "Correo:", value = email)

            Spacer(modifier = Modifier.height(60.dp))

            // Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isChofer) "Ganancias hoy:" else "Saldo disponible:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A6572)
                )
                Text(text = "$saldo BS", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Transactions Box
            Text(
                text = if (isChofer) "Últimos cobros:" else "Lista de transacciones:",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A6572)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Esto permite que la caja crezca y el botón se mantenga abajo
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Realiza una transacción,\naprovecha la App!",
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(transactions) { transaction ->
                            TransactionItem(transaction, isChofer)
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isChofer) Color(0xFF2E7D32) else Color(0xFF424242)
                )
            ) {
                Text(
                    text = if (isChofer) "COBRAR PASAJE" else "PAGAR PASAJE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A6572))
        Text(text = value, fontSize = 14.sp, color = Color.DarkGray)
    }
}

@Composable
fun TransactionItem(transaction: TransactionResponse, isChofer: Boolean) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val label = if (transaction.tipo == "Pago_Pasaje") {
                if (isChofer) "Cobro Pasaje" else "Pago Pasaje"
            } else {
                "Recarga Saldo"
            }
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${if (isChofer || transaction.tipo == "Recarga_Saldo") "+" else "-"}${transaction.monto} BS", 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold,
                color = if (isChofer || transaction.tipo == "Recarga_Saldo") Color(0xFF2E7D32) else Color.Red
            )
        }
        Text(
            text = transaction.fecha,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreviewEmpty() {
    HomeScreen(transactions = emptyList())
}
