package com.example.app_android

import com.example.app_android.data.RegisterRequest
import org.junit.Test
import org.junit.Assert.*

class ModelsTest {

    @Test
    fun `verificar que RegisterRequest guarda los datos correctamente`() {
        val request = RegisterRequest(
            nombres = "Juan",
            apellidos = "Perez",
            carnetIdentidad = "1234567",
            correoElectronico = "juan@example.com",
            fechaNacimiento = "1990-01-01",
            contrasena = "secret123",
            rolId = 1
        )

        assertEquals("Juan", request.nombres)
        assertEquals("juan@example.com", request.correoElectronico)
    }

    @Test
    fun `esta prueba va a fallar a proposito`() {
        // 1. Preparación
        val valorEsperado = 100
        val valorReal = 50 + 25 // Esto da 75

        // 2. Verificación (Dará error porque 100 != 75)
        assertEquals("El cálculo debería ser correcto", valorEsperado, valorReal)
    }
}
