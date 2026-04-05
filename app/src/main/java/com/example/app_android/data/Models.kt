package com.example.app_android.data

import com.google.gson.annotations.SerializedName

// Respuesta del Usuario
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: Any? = null
)

// Roles
data class RoleResponse(
    val id: Int,
    val nombre: String
)

data class RolesWrapperResponse(
    val data: List<RoleResponse>
)

// Auth
data class LoginRequest(val email: String, val contrasena: String)
data class LoginResponse(
    val message: String? = null,
    @SerializedName("access_token") val accessToken: String,
    val user: UserResponse
)

// Registro
data class RegisterRequest(
    val nombres: String,
    val apellidos: String,
    val carnetIdentidad: String,
    val correoElectronico: String,
    val fechaNacimiento: String,
    val contrasena: String,
    val rolId: Int
)

// Dispositivo Móvil (Tabla dispositivo_movil)
data class DispositivoMovilRequest(
    @SerializedName("id_dispositivo") val idDispositivo: String,
    @SerializedName("id_usuario") val idUsuario: String,
    @SerializedName("modelo_app") val modeloApp: String,
    @SerializedName("marca_modelo") val marcaModelo: String,
    @SerializedName("uid_nfc") val uidNfc: String
)

// Tarjeta NFC (Tabla tarjeta_nfc)
data class TarjetaNfcRequest(
    @SerializedName("uid_nfc") val uidNfc: String,
    @SerializedName("id_usuario") val idUsuario: String
)

data class DispositivoMovilResponse(val status: String, val mensaje: String? = null)

// Pagos
data class PagoNfcRequest(@SerializedName("uid_nfc") val uidNfc: String, val monto: Double = 1.50)
data class PagoNfcResponse(
    val status: String,
    val mensaje: String,
    val estudiante: String? = null,
    @SerializedName("monto_cobrado") val montoCobrado: Double? = null,
    @SerializedName("saldo_restante") val saldoRestante: Double? = null
)

// Cuentas
data class CuentaResponse(val saldo: String)
data class CuentaWrapperResponse(val status: String, val data: CuentaResponse)

// Transacciones
data class TransactionWrapperResponse(val status: String, val data: List<TransactionResponse>)

data class TransactionResponse(
    val id: Int,
    val monto: String? = "0.00",
    val tipo: String? = "Pago_Pasaje", // The backend resource is missing this field, so we default it
    val fecha: String? = "Sin fecha",
    val idCuentaOrigen: Int? = null,
    val idCuentaDestino: Int? = null
)
