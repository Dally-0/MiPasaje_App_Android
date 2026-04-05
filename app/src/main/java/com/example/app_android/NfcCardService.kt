package com.example.app_android

import android.media.MediaPlayer
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import android.provider.Settings
import com.example.app_android.data.NfcPaymentBridge

class NfcCardService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return RESPONSE_FAIL

        // 1. EL CHOFER SOLICITA IDENTIFICACIÓN
        if (isSelectAidApdu(commandApdu)) {
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ID"
            Log.d("HCE", "Identificando estudiante...")
            return deviceId.toByteArray() + RESPONSE_OK
        }

        // 2. EL CHOFER ENVÍA EL RESULTADO DEL SERVIDOR (Comando personalizado)
        // [0x00, 0x55] = SUCCESS, [0x00, 0xEE] = ERROR
        if (commandApdu.size >= 2 && commandApdu[0] == 0x00.toByte()) {
            when (commandApdu[1]) {
                0x55.toByte() -> { // ÉXITO
                    val mensajeExito = if (commandApdu.size > 2) String(commandApdu.copyOfRange(2, commandApdu.size)) else ""
                    reproducirSonido(true)
                    NfcPaymentBridge.notifySuccess(mensajeExito)
                    return RESPONSE_OK
                }
                0xEE.toByte() -> { // ERROR (Saldo insuficiente, etc)
                    val mensajeError = if (commandApdu.size > 2) String(commandApdu.copyOfRange(2, commandApdu.size)) else "Error en pago"
                    reproducirSonido(false)
                    NfcPaymentBridge.notifyError(mensajeError)
                    return RESPONSE_OK
                }
            }
        }

        return RESPONSE_FAIL
    }

    private fun reproducirSonido(success: Boolean) {
        try {
            val resId = if (success) Settings.System.DEFAULT_NOTIFICATION_URI 
                        else Settings.System.DEFAULT_ALARM_ALERT_URI
            val mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        } catch (e: Exception) { }
    }

    override fun onDeactivated(reason: Int) { }

    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        return apdu.size >= 2 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte()
    }

    companion object {
        private val RESPONSE_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val RESPONSE_FAIL = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    }
}
