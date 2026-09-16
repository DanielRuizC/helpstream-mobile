package com.example.helpstream_mobile

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// 1. Modelos de datos
data class TicketCreate(
    val usuario_id: Int,
    val descripcion: String
)

data class TicketResponse(
    val id: Int,
    val usuario_id: Int,
    val descripcion: String,
    val fecha_creacion: String,
    val palabras_clave: List<String>,
    val criticidad: String? = null
)

data class TicketRespuesta(
    val id: Int,
    val usuario_id: Int,
    val descripcion: String,
    val fecha_creacion: String,
    val estado: String,
    val comentario_tecnico: String? = null
)

// 2. La interfaz de Retrofit que define el endpoint
interface ApiService {
    @Multipart
    @POST("/tickets/")
    suspend fun crearTicket(
        @Part("descripcion") descripcion: RequestBody,
        @Part("usuario_id") usuarioId: RequestBody,
        @Part archivo: MultipartBody.Part?
    ): Response<TicketResponse>

    @GET("videos/")
    suspend fun obtenerVideosTutoriales(@Query("tags") tags: String? = null): Response<List<VideoTutorial>>

    @POST("tickets/{ticket_id}/resolver-autoatencion")
    suspend fun resolverTicketAutoatencion(@Path("ticket_id") ticketId: Int): Response<Unit>

    @GET("tickets/usuario/{usuario_id}")
    suspend fun obtenerMisSolicitudes(@Path("usuario_id") usuarioId: Int): Response<List<TicketRespuesta>>
}

// 3. El cliente configurado para el emulador
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}