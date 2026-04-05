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
                            onCancel = { 
                                isNfcActive = false 
                                // Al cerrar el modo NFC, refrescamos datos
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

                                                val roleObj = loginData.user.role
                                                val roleId = when {
                                                    roleObj is Number -> roleObj.toInt()
                                                    roleObj.toString().contains("3") -> 3
                                                    else -> 2
                                                }
                                                
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

                if (roleId == 2) {
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
                    val roleObj = response.body()!!.role
                    val roleId = when {
                        roleObj is Number -> roleObj.toInt()
                        roleObj.toString().contains("3") -> 3
                        roleObj == "Chofer" -> 3
                        else -> 2
                    }
                    
                    if (roleId == 1) {
                        Toast.makeText(this@MainActivity, "Acceso denegado a Administradores", Toast.LENGTH_LONG).show()
                    } else {
                        currentUserRole = roleId
                        realizarRegistrosAutomaticos(token, userId, roleId)
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
                    currentTransactions = response.body()!!
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error fetching transactions: ${e.message}")
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
        isProcessingNfc = true
        nfcStatusMessage = "Leyendo..."
        
        lifecycleScope.launch {
            try {
                val isoDep = IsoDep.get(tag)
                var finalUid = tag.id.joinToString("") { "%02x".format(it) }

                if (isoDep != null) {
                    try {
                        isoDep.connect()
                        val selectCommand = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(), 
                            0xF0.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(), 0x05.toByte(), 0x06.toByte())
                        val res = isoDep.transceive(selectCommand)
                        if (res.size >= 2 && res[res.size - 2] == 0x90.toByte()) {
                            finalUid = String(res.copyOfRange(0, res.size - 2))
                        }
                        isoDep.close()
                    } catch (e: Exception) { }
                }

                val response = RetrofitClient.instance.procesarPago("Bearer $currentToken", PagoNfcRequest(uidNfc = finalUid))
                
                if (response.isSuccessful) {
                    playSound(true)
                    val data = response.body()
                    currentSaldo = "%.2f".format(data?.saldoRestante ?: 0.0)
                    nfcStatusMessage = "¡Cobro exitoso!\nEstudiante: ${data?.mensaje}"
                } else {
                    playSound(false)
                    val errorBody = response.errorBody()?.string() ?: ""
                    try {
                        val errorData = Gson().fromJson(errorBody, PagoNfcResponse::class.java)
                        nfcStatusMessage = "Error: ${errorData.mensaje}"
                    } catch (e: Exception) {
                        nfcStatusMessage = "Error ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                playSound(false)
                nfcStatusMessage = "Error de red"
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
        } catch (e: Exception) { }
    }
}
