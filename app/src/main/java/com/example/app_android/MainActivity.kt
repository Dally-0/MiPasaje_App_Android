package com.example.app_android

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    
    private var isNfcActive by mutableStateOf(false)
    private var nfcStatusMessage by mutableStateOf("Esperando contacto...")
    private var isProcessingNfc by mutableStateOf(false)
    private var currentToken by mutableStateOf("")
    private var currentUserRole by mutableIntStateOf(2)
    private var currentSaldo by mutableStateOf("0.00")
    private var currentTransactions by mutableStateOf<List<TransactionResponse>>(emptyList())
    private var loggedInUser by mutableStateOf<UserResponse?>(null)
    
    // Variables de Cooldown NFC
    private var lastProcessedUid: String? = null
    private var lastProcessedTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        enableEdgeToEdge()
        
        setContent {
            App_AndroidTheme {
                var currentScreen by remember { mutableStateOf("login") }

                // Monitorear el puente NFC para el estudiante
                LaunchedEffect(NfcPaymentBridge.status) {
                    when(NfcPaymentBridge.status) {
                        "exitoso" -> {
                            val msg = NfcPaymentBridge.successMessage
                            nfcStatusMessage = if (msg.isNotEmpty()) "¡Pago Realizado!\n$msg" else "¡Pago Realizado!"
                        }
                        "error" -> nfcStatusMessage = "Error: ${NfcPaymentBridge.errorMessage}"
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isNfcActive) {
                        NfcTransactionScreen(
                            rolId = currentUserRole,
                            statusMessage = nfcStatusMessage,
                            isProcessing = isProcessingNfc,
                            onCancel = { 
                                isNfcActive = false 
                                nfcStatusMessage = "Esperando contacto..."
                                NfcPaymentBridge.reset()
                                loggedInUser?.let { 
                                    fetchSaldo(currentToken, it.id)
                                    fetchTransactions(currentToken, it.id)
                                }
                            }
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
                                                currentToken = loginData.accessToken
                                                loggedInUser = loginData.user
                                                
                                                fetchProfileAndNavigate(loginData.accessToken, loginData.user.id) {
                                                    currentScreen = "home"
                                                }
                                            } else {
                                                val errorBody = response.errorBody()?.string() ?: ""
                                                Log.e("API_ERROR", "Error login: $errorBody")
                                                val errorMsg = if (response.code() == 401) "Correo o contraseña incorrectos" else "Error del servidor: ${response.code()}"
                                                Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
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
                                                currentToken = loginData.accessToken
                                                loggedInUser = loginData.user
                                                
                                                Toast.makeText(this@MainActivity, "¡Registro Exitoso!", Toast.LENGTH_SHORT).show()

                                                val roleId = extractRoleId(loginData.user.role)
                                                
                                                currentUserRole = roleId
                                                realizarRegistrosAutomaticos(loginData.accessToken, loginData.user.id, roleId)
                                                fetchSaldo(loginData.accessToken, loginData.user.id)
                                                fetchTransactions(loginData.accessToken, loginData.user.id)
                                                
                                                currentScreen = "home"
                                            } else {
                                                val errorBody = response.errorBody()?.string() ?: ""
                                                Log.e("API_ERROR", "Error registro: $errorBody")
                                                Toast.makeText(this@MainActivity, "Error en registro: $errorBody", Toast.LENGTH_LONG).show()
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
                                        transactions = currentTransactions,
                                        onActionClick = { 
                                            if (nfcAdapter == null) {
                                                Toast.makeText(this@MainActivity, "NFC no disponible", Toast.LENGTH_LONG).show()
                                            } else if (!nfcAdapter!!.isEnabled) {
                                                Toast.makeText(this@MainActivity, "Por favor active el NFC", Toast.LENGTH_LONG).show()
                                            } else {
                                                isNfcActive = true
                                                nfcStatusMessage = "Esperando contacto..."
                                                NfcPaymentBridge.reset()
                                            }
                                        },
                                        onRefreshClick = {
                                            fetchSaldo(currentToken, user.id)
                                            fetchTransactions(currentToken, user.id)
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

    private fun extractRoleId(roleObj: Any?): Int {
        if (roleObj == null) return 2
        if (roleObj is Number) return roleObj.toInt()
        val roleStr = roleObj.toString()
        return when {
            roleStr.contains("Chofer", ignoreCase = true) -> 5
            roleStr.contains("Universidad", ignoreCase = true) -> 4
            roleStr.contains("Secundaria", ignoreCase = true) -> 3
            roleStr.contains("Primaria", ignoreCase = true) -> 2
            roleStr.contains("Administrador", ignoreCase = true) -> 1
            else -> roleStr.toIntOrNull() ?: 2
        }
    }

    private fun realizarRegistrosAutomaticos(token: String, userId: String, roleId: Int) {
        lifecycleScope.launch {
            try {
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                
                val requestDispositivo = DispositivoMovilRequest(
                    idDispositivo = androidId,
                    idUsuario = userId,
                    modeloApp = Build.MODEL.take(20),
                    marcaModelo = "${Build.MANUFACTURER} ${Build.MODEL}",
                    uidNfc = androidId
                )
                RetrofitClient.instance.registrarDispositivo("Bearer $token", requestDispositivo)

                // Si es estudiante (IDs 2, 3, 4), vinculamos la tarjeta NFC
                if (roleId in 2..4) {
                    val requestTarjeta = TarjetaNfcRequest(uidNfc = androidId, idUsuario = userId)
                    RetrofitClient.instance.vincularTarjetaNfc("Bearer $token", requestTarjeta)
                }
            } catch (e: Exception) { }
        }
    }

    private fun fetchProfileAndNavigate(token: String, userId: String, onSuccess: () -> Unit) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getUserProfile("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val roleId = extractRoleId(response.body()!!.role)
                    
                    if (roleId == 1) {
                        Toast.makeText(this@MainActivity, "Acceso denegado a Administradores", Toast.LENGTH_SHORT).show()
                    } else {
                        currentUserRole = roleId
                        fetchSaldo(token, userId)
                        fetchTransactions(token, userId)
                        onSuccess()
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun fetchSaldo(token: String, userId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getCuenta("Bearer $token", userId)
                if (response.isSuccessful && response.body() != null) {
                    currentSaldo = response.body()!!.data.saldo
                }
            } catch (e: Exception) { }
        }
    }

    private fun fetchTransactions(token: String, userId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getTransacciones("Bearer $token", userId)
                if (response.isSuccessful && response.body() != null) {
                    currentTransactions = response.body()!!.data
                }
            } catch (e: Exception) { }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNfcActive && NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
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
        if (isProcessingNfc) return
        isProcessingNfc = true
        nfcStatusMessage = "Procesando..."

        lifecycleScope.launch {
            try {
                var finalUid = tag.id.joinToString("") { "%02x".format(it) }
                var tagResponded = false
                val isoDep = IsoDep.get(tag)

                if (isoDep != null) {
                    try {
                        isoDep.connect()
                        isoDep.timeout = 5000 // 5 segundos para que la conexión NFC no se pierda durante la llamada API
                        val selectCommand = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(), 
                            0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte())
                        val res = isoDep.transceive(selectCommand)
                        if (res.size >= 2 && res[res.size - 2] == 0x90.toByte()) {
                            finalUid = String(res.copyOfRange(0, res.size - 2))
                            tagResponded = true
                        }
                    } catch (e: Exception) { }
                }

                // Opción 2: Evitar rebotes en menos de 10 segundos
                val currentTime = System.currentTimeMillis()
                if (finalUid == lastProcessedUid && (currentTime - lastProcessedTime) < 10000) {
                    nfcStatusMessage = "Cobro ya realizado en este dispositivo recientemente."
                    if (isoDep != null && isoDep.isConnected) isoDep.close()
                    isProcessingNfc = false
                    return@launch
                }
                
                lastProcessedUid = finalUid
                lastProcessedTime = currentTime

                val response = RetrofitClient.instance.procesarPago("Bearer $currentToken", PagoNfcRequest(uidNfc = finalUid))
                
                if (response.isSuccessful) {
                    playSound()
                    val data = response.body()
                    val nombreEstudiante = data?.estudiante ?: "Desconocido"
                    val montoCobrado = "%.2f".format(data?.montoCobrado ?: 0.0)
                    
                    // Solo mostramos información relevante para el chofer
                    nfcStatusMessage = "¡Cobro exitoso!\nEstudiante: $nombreEstudiante\nCobro: Bs. $montoCobrado"
                    
                    // Como el chofer es quien cobra, actualizamos su saldo desde el servidor para reflejar el incremento real:
                    loggedInUser?.let { fetchSaldo(currentToken, it.id); fetchTransactions(currentToken, it.id) }

                    // SI ES UN CELULAR, LE ENVIAMOS EL COMANDO DE ÉXITO (0x00, 0x55) MAS EL RESUMEN DEL COBRO AL ESTUDIANTE
                    if (tagResponded && isoDep != null && isoDep.isConnected) {
                        try {
                            val msgEstudiante = "Monto: Bs. $montoCobrado | Te quedan: Bs. ${"%.2f".format(data?.saldoRestante ?: 0.0)}"
                            val exitoArray = byteArrayOf(0x00.toByte(), 0x55.toByte()) + msgEstudiante.toByteArray()
                            isoDep.transceive(exitoArray)
                        } catch (e: Exception) {}
                    }
                    
                    // Opción 1: Redirigir al menú principal automáticamente después de 2.5 seg
                    kotlinx.coroutines.delay(2500)
                    isNfcActive = false
                    nfcStatusMessage = "Esperando contacto..."
                } else {
                    playSound()
                    val errorBody = response.errorBody()?.string() ?: ""
                    val errorMsg: String = try {
                        val errorData = Gson().fromJson(errorBody, PagoNfcResponse::class.java)
                        errorData.mensaje
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    nfcStatusMessage = "Error: $errorMsg"
                    
                    // SI ES UN CELULAR, LE ENVIAMOS EL COMANDO DE ERROR (0x00, 0xEE) + MENSAJE
                    if (tagResponded && isoDep != null && isoDep.isConnected) {
                        try {
                            val errorBytes = byteArrayOf(0x00.toByte(), 0xEE.toByte()) + errorMsg.toByteArray()
                            isoDep.transceive(errorBytes)
                        } catch (e: Exception) {}
                    }
                }
                
                if (isoDep != null && isoDep.isConnected) isoDep.close()

            } catch (e: Exception) {
                playSound()
                nfcStatusMessage = "Error de red"
            } finally {
                isProcessingNfc = false
            }
        }
    }

    private fun playSound() {
        try {
            val resId = Settings.System.DEFAULT_NOTIFICATION_URI
            val mp = MediaPlayer.create(this, resId)
            mp.start()
            mp.setOnCompletionListener { it.release() }
        } catch (e: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
}
