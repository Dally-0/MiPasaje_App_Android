package com.example.app_android.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Un objeto simple que sirve de puente entre el Servicio (NfcCardService)
 * y la Interfaz de Usuario (MainActivity).
 */
object NfcPaymentBridge {
    // Variable reactiva que la UI puede observar
    var paymentSuccess by mutableStateOf(false)
    var status by mutableStateOf("esperando") // "esperando", "exitoso", "error"
    var errorMessage by mutableStateOf("")
    var successMessage by mutableStateOf("")

    fun reset() {
        paymentSuccess = false
        status = "esperando"
        errorMessage = ""
        successMessage = ""
    }

    fun notifySuccess(msg: String = "") {
        paymentSuccess = true
        status = "exitoso"
        successMessage = msg
    }

    fun notifyError(message: String) {
        paymentSuccess = false
        status = "error"
        errorMessage = message
    }
}
