<div align="center">

# 🚌 MiPasaje — App Android

### Sistema Móvil de Pago y Cobro de Pasajes de Transporte Público mediante NFC (HCE)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android%20SDK-36-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![NFC HCE](https://img.shields.io/badge/NFC-Host%20Card%20Emulation-FF6F00?style=for-the-badge&logo=nfc&logoColor=white)](https://developer.android.com/guide/topics/connectivity/nfc/hce)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.x-009688?style=for-the-badge&logo=square)](https://square.github.io/retrofit/)

</div>

---

## 📋 Descripción del Proyecto

**MiPasaje App Android** es una plataforma nativa de cobro y pago digital de transporte público diseñada para eliminar el uso de efectivo y simplificar el abordaje en autobuses.

A través de tecnología **NFC HCE (Host Card Emulation)**, la aplicación permite que un teléfono inteligente actúe como una **tarjeta virtual de transporte**, permitiendo a los usuarios pagar su pasaje simplemente acercando su dispositivo al teléfono del chofer o terminal de cobro.

---

## ✨ Funcionalidades Clave

| Módulo | Descripción |
|--------|-------------|
| 📱 **Tarjeta Virtual NFC (HCE)** | El dispositivo del estudiante/pasajero emula una tarjeta NFC inteligente (`HostApduService`) transmitiendo un identificador único seguro. |
| 💳 **Terminal de Cobro para Choferes** | El dispositivo del chofer actúa como lector NFC usando `ForegroundDispatch` e `IsoDep`, leyendo tarjetas o teléfonos pasados por el sensor. |
| 🔄 **Protocolo APDU Bidireccional** | Comunicación a bajo nivel ISO/IEC 7816-4 para solicitar identificación y retornar la respuesta del servidor (éxito con saldo restante o error con sonido descriptivo). |
| 🛡️ **Protección Anti-Rebote (Cooldown)** | Previene cobros duplicados en menos de 10 segundos para la misma tarjeta NFC o dispositivo. |
| 🎓 **Tarifas Diferenciadas por Rol** | Soporte para múltiples roles de usuario (Primaria, Secundaria, Universidad, Chofer) con cálculo automático de tarifas reducidas o completas. |
| 💰 **Gestión de Saldo y Historial** | Consulta de saldo en tiempo real y registro histórico de transacciones realizadas y cobradas. |
| 🎨 **UI Moderna en Jetpack Compose** | Interfaz responsiva con animación de lectura NFC, modo oscuro/claro y tokens de **Material 3**. |

---

## 🛠️ Stack Tecnológico

### Android & UI
- **Kotlin**: Lenguaje principal con uso intensivo de Corrutinas (`lifecycleScope`) y Flow.
- **Jetpack Compose + Material 3**: UI declarativa, temas dinámicos y estados interactivos (`mutableStateOf`).
- **Edge-to-Edge API**: Diseño inmersivo compatible con las últimas guías de Android 15/16.

### NFC & Hardware Integration
- **Android HCE (`HostApduService`)**: Emulación de tarjeta inteligente en segundo plano sin chip Secure Element dedicado.
- **NfcAdapter & IsoDep**: Captura de eventos `ACTION_TAG_DISCOVERED` y comunicación APDU sobre protocolo ISO 14443-4.
- **MediaPlayer API**: Retroalimentación sonora inmediata tras confirmación o fallo en la transacción.

### Red y Persistencia
- **Retrofit 2 + Gson**: Cliente HTTP de alto rendimiento para interactuar con la API REST.
- **OkHttp Logging Interceptor**: Monitoreo y depuración de peticiones HTTP en desarrollo.

---

## ⚙️ Arquitectura NFC y Protocolo APDU

El sistema implementa un canal de comunicación de bajo nivel a través del protocolo **ISO/IEC 7816-4**:

```
+------------------------+                     +------------------------+
|   Teléfono Chofer      |                     |  Teléfono Estudiante   |
| (Lector ISO-DEP)       |                     | (HostApduService HCE)  |
+------------------------+                     +------------------------+
            |                                               |
            | ------- 1. SELECT AID (0x00 0xA4...) --------> |
            | <------ 2. ANDROID_ID + SW_OK (0x9000) ------- |
            |                                               |
  [Consulta API REST /cobro]                                |
            |                                               |
            | ------- 3. APDU Resultado (0x00 0x55/0xEE) --> |
            |                                         [Sonido + UI]
```

### Comandos APDU Implementados:
- **`SELECT AID`** (`0x00 0xA4 0x04 0x00`): El chofer solicita la identificación del estudiante. La app responde con el `ANDROID_ID` codificado en bytes + `0x9000` (`RESPONSE_OK`).
- **`PAGO ÉXITO`** (`0x00 0x55`): El chofer notifica la aprobación del cobro desde la API. La app del pasajero reproduce un sonido de confirmación y muestra el saldo restante en pantalla via `NfcPaymentBridge`.
- **`PAGO ERROR`** (`0x00 0xEE`): Notifica fondos insuficientes o tarjeta inválida con feedback audible de alerta.

---

## 📂 Estructura del Proyecto

```text
app/src/main/java/com/example/app_android/
├── MainActivity.kt               # Control central de pantallas, ForegroundDispatch y procesamiento NFC
├── NfcCardService.kt             # Servicio HostApduService (HCE) para emulación de tarjeta virtual
├── data/
│   ├── ApiService.kt             # Cliente Retrofit y endpoints REST (auth, cobro, saldo, transacciones)
│   ├── Models.kt                 # Data classes de peticiones/respuestas (DTOs)
│   └── NfcPaymentBridge.kt       # Puente reactivo de estados entre HCE service y Compose UI
└── ui/
    ├── HomeScreen.kt             # Dashboard de usuario (Saldo, botón de pago/cobro y transacciones)
    ├── LoginScreen.kt            # Pantalla de inicio de sesión
    ├── SignUpScreen.kt           # Registro de usuarios y asignación de roles
    ├── NfcTransactionScreen.kt   # Pantalla modal con estado visual de lectura NFC
    └── theme/                    # Tokens de diseño, colores y tipografía de Material 3
```

---

## 🚀 Instalación y Configuración

### Requisitos Previos
- **Android Studio** Ladybug (o superior)
- **JDK 11** o superior
- Dispositivo Android físico con soporte **NFC** (SDK mínimo 24 / Android 7.0+)

### Pasos
1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/MiPasaje_App_Android.git
   ```
2. **Abrir en Android Studio:**
   Abre la carpeta `MiPasaje_App_Android` en Android Studio y espera a que Gradle sincronice las dependencias.
3. **Configurar IP del Backend:**
   Edita la URL base del servidor en `data/ApiService.kt`:
   ```kotlin
   private const val BASE_URL = "http://TU_IP_LOCAL:8000/api/"
   ```
4. **Compilar y Ejecutar:**
   Conecta un dispositivo físico con NFC habilitado y presiona **Run** (`Shift + F10`).

---

<div align="center">

Desarrollado para el sistema **MiPasaje** 🚌

</div>
