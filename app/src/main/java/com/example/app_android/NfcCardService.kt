package com.example.app_android

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import android.provider.Settings

class NfcCardService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return SELECT_AID_RESPONSE_FAIL

        // Si el lector (Chofer) envía el comando para seleccionar nuestra app (AID)
        if (isSelectAidApdu(commandApdu)) {
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            Log.d("HCE", "Enviando ID de dispositivo: $deviceId")
            
            // Enviamos el ID del dispositivo como respuesta (Convertido a bytes)
            return deviceId.toByteArray() + SELECT_AID_RESPONSE_OK
        }

        return SELECT_AID_RESPONSE_FAIL
    }

    override fun onDeactivated(reason: Int) {
        Log.d("HCE", "Deactivated: $reason")
    }

    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        return apdu.size >= 2 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte()
    }

    companion object {
        private val SELECT_AID_RESPONSE_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SELECT_AID_RESPONSE_FAIL = byteArrayOf(0x6F.toByte(), 0x00.toByte())
    }
}
