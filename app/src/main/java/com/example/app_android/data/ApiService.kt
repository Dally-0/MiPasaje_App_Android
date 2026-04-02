package com.example.app_android.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Headers

interface ApiService {
    @Headers("Accept: application/json")
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @Headers("Accept: application/json")
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("dispositivo-movil")
    suspend fun registrarDispositivo(
        @Header("Authorization") token: String,
        @Body request: DispositivoMovilRequest
    ): Response<DispositivoMovilResponse>

    @Headers("Accept: application/json")
    @POST("cobro")
    suspend fun procesarPago(
        @Header("Authorization") token: String,
        @Body request: PagoNfcRequest
    ): Response<PagoNfcResponse>
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.100.30:8000/api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}
