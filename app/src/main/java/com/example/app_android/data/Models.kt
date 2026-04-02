package com.example.app_android.data

import com.google.gson.annotations.SerializedName

// Respuesta del Usuario desde AuthController (id, name, email, role)
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: Int? = null
)

// Request para Login
data class LoginRequest(
    val email: String,
    val contrasena: String
)

// Respuesta de Autenticación (Login y Registro ahora devuelven esto)
data class LoginResponse(
    val message: String? = null,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserResponse
)

// Request para Registro (basado en AuthController@register)
data class RegisterRequest(
    val nombres: String,
    val apellidos: String,
    val carnetIdentidad: String,
    val correoElectronico: String,
    val fechaNacimiento: String,
    val contrasena: String,
    val rolId: Int
)

// Request para registrar dispositivo móvil
data class DispositivoMovilRequest(
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("modelo_app") val modeloApp: String,
    @SerializedName("marca_modelo") val marcaModelo: String
)

// Respuesta para registro de dispositivo móvil
data class DispositivoMovilResponse(
    val status: String,
    val mensaje: String? = null,
    val data: Any? = null
)

// Request para procesar pago via NFC
data class PagoNfcRequest(
    @SerializedName("uid_nfc") val uidNfc: String,
    val monto: Double = 1.50
)

// Respuesta de pago
data class PagoNfcResponse(
    val status: String,
    val mensaje: String,
    val saldo_restante: Double? = null
)
