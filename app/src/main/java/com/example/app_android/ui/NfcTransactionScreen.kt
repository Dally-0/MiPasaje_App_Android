package com.example.app_android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NfcTransactionScreen(
    rolId: Int,
    statusMessage: String,
    isProcessing: Boolean,
    onCancel: () -> Unit
) {
    val isChofer = rolId == 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = if (isChofer) Color(0xFF2E7D32) else Color(0xFF1976D2)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isChofer) "MODO COBRADOR" else "MODO PAGO",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isChofer) 
                "Acerque el celular del estudiante o su tarjeta NFC" 
            else 
                "Acerque su celular al terminal del chofer para pagar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = if (isChofer) Color(0xFF2E7D32) else Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = statusMessage,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (statusMessage.contains("Error") || statusMessage.contains("insuficiente")) 
                Color.Red 
            else if (statusMessage.contains("exitoso"))
                Color(0xFF2E7D32)
            else
                Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onCancel,
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray, 
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(200.dp).height(50.dp)
        ) {
            Text("Cerrar / Cancelar")
        }
    }
}
