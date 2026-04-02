package com.example.app_android

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.app_android.data.*
import com.example.app_android.ui.HomeScreen
import com.example.app_android.ui.LoginScreen
import com.example.app_android.ui.SignUpScreen
import com.example.app_android.ui.NfcTransactionScreen
import com.example.app_android.ui.theme.App_AndroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    
    // Estados para la transacción NFC
    private var isNfcActive by mutableStateOf(false)
    private var nfcStatusMessage by mutableStateOf("Esperando contacto...")
    private var isProcessingNfc by mutableStateOf(false)
    private var currentToken by mutableStateOf("")
    private var currentUserRole by mutableIntStateOf(2)
    private var currentSaldo by mutableStateOf("0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        
        setContent {
            App_AndroidTheme {
                var currentScreen by remember { mutableStateOf("login") }
                var loggedInUser by remember { mutableStateOf<UserResponse?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isNfcActive) {
                        NfcTransactionScreen(
                            rolId = currentUserRole,
                            statusMessage = nfcStatusMessage,
                            isProcessing = isProcessingNfc,
                            onCancel = { isNfcActive = false }
                        )
                    } else {
                        when (currentScreen) {
                            "login" -> LoginScreen(
                                onLoginSuccess = { email, password ->
                                    lifecycleScope.launch {
                                        try {
                                            val response = RetrofitClient.instance.login(LoginRequest(email, password))
                                            if (response.isSuccessful && response.body() != null) {
                                                val loginData = response.body()!!
                                                if (loginData.user.role == 1) {
                                                    Toast.makeText(this@MainActivity, "Acceso denegado: Solo Estudiantes o Choferes", Toast.LENGTH_LONG).show()
                                                } else {
                                                    loggedInUser = loginData.user
                                                    currentToken = loginData.accessToken
                                                    currentUserRole = loginData.user.role ?: 2
                                                    registrarDispositivo(loginData.accessToken, loginData.user.id)
                                                    currentScreen = "home"
                                                }
                                            } else {
                                                Toast.makeText(this@MainActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onNavigateToSignUp = { currentScreen = "signup" },
                                modifier = Modifier.padding(innerPadding)
                            )
                            "signup" -> SignUpScreen(
                                onSignUpRequest = { registerData ->
                                    lifecycleScope.launch {
                                        try {
                                            val response = RetrofitClient.instance.register(registerData)
                                            if (response.isSuccessful && response.body() != null) {
                                                val loginData = response.body()!!
                                                if (loginData.user.role == 1) {
                                                    Toast.makeText(this@MainActivity, "Admin no puede usar la app móvil", Toast.LENGTH_LONG).show()
                                                    currentScreen = "login"
                                                } else {
                                                    currentToken = loginData.accessToken
                                                    currentUserRole = loginData.user.role ?: 2
                                                    registrarDispositivo(loginData.accessToken, loginData.user.id)
                                                    loggedInUser = loginData.user
                                                    currentScreen = "home"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Error de red", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onNavigateToLogin = { currentScreen = "login" },
                                modifier = Modifier.padding(innerPadding)
                            )
                            "home" -> {
                                loggedInUser?.let { user ->
                                    HomeScreen(
                                        userName = user.name.split(" ").firstOrNull() ?: user.name,
                                        fullNames = user.name,
                                        email = user.email,
                                        rolId = currentUserRole,
                                        saldo = currentSaldo,
                                        onActionClick = { 
                                            if (nfcAdapter == null) {
                                                Toast.makeText(this@MainActivity, "NFC no disponible en este dispositivo", Toast.LENGTH_LONG).show()
                                            } else if (!nfcAdapter!!.isEnabled) {
                                                Toast.makeText(this@MainActivity, "Por favor active el NFC", Toast.LENGTH_LONG).show()
                                            } else {
                                                isNfcActive = true
                                                nfcStatusMessage = "Esperando contacto..."
                                            }
                                        },
                                        onRefreshClick = {
                                            // Lógica para actualizar saldo (puedes llamar a un endpoint de perfil)
                                            Toast.makeText(this@MainActivity, "Saldo actualizado", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    private fun enableNfcForegroundDispatch() {
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNfcActive && currentUserRole == 3 && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let { processNfcTag(it) }
        }
    }

    private fun processNfcTag(tag: Tag) {
        val tagId = tag.id.joinToString("") { "%02x".format(it) }
        isProcessingNfc = true
        nfcStatusMessage = "Procesando pago..."
        
        lifecycleScope.launch {
            try {
                // El ID de la tarjeta física o del celular emulado se envía como uidNfc
                val response = RetrofitClient.instance.procesarPago(
                    "Bearer $currentToken",
                    PagoNfcRequest(uidNfc = tagId)
                )
                
                if (response.isSuccessful) {
                    playSound(true)
                    val data = response.body()
                    currentSaldo = data?.saldo_restante?.toString() ?: currentSaldo
                    nfcStatusMessage = "¡Pago exitoso!\nSaldo actualizado."
                } else {
                    playSound(false)
                    val errorBody = response.errorBody()?.string()
                    nfcStatusMessage = if (errorBody?.contains("saldo") == true) {
                        "Error: Saldo insuficiente"
                    } else {
                        "Error en la transacción"
                    }
                }
            } catch (e: Exception) {
                playSound(false)
                nfcStatusMessage = "Error de conexión"
            } finally {
                isProcessingNfc = false
            }
        }
    }

    private fun playSound(success: Boolean) {
        try {
            val resId = if (success) android.provider.Settings.System.DEFAULT_NOTIFICATION_URI 
                        else android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            val mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registrarDispositivo(token: String, userId: String) {
        lifecycleScope.launch {
            try {
                val request = DispositivoMovilRequest(
                    idUsuario = userId,
                    modeloApp = Build.MODEL.take(20),
                    marcaModelo = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
                RetrofitClient.instance.registrarDispositivo("Bearer $token", request)
            } catch (e: Exception) { /* Silently fail device reg */ }
        }
    }
}
