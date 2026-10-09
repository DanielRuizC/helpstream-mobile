package com.example.helpstream_mobile

import android.content.Context
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// 1. Modelos de datos
data class TicketCreate(
    val usuario_id: Int,
    val descripcion: String,
    val correo_solicitante: String,
    val sede: String,
    val piso: String
)

data class Creador(
    val nombre: String,
    val apellidos: String,
    val correo: String,
    val telefono: String,
    val anexo: String
)

data class TicketResponse(
    val id: Int,
    val usuario_id: Int,
    val descripcion: String,
    val fecha_creacion: String,
    val palabras_clave: List<String>,
    val criticidad: String? = null,
    val sede: String,
    val piso: String,
    val creador: Creador
)

data class TicketRespuesta(
    val id: Int,
    val usuario_id: Int,
    val descripcion: String,
    val fecha_creacion: String,
    val estado: String,
    val comentario_tecnico: String? = null,
    val sede: String,
    val piso: String,
    val creador: Creador
)

data class UsuarioLogin(
    val id: Int,
    val nombre: String? = null,
    val apellidos: String? = null,
    val correo: String? = null,
    val rol_id: Int? = null,
    val nombre_usuario: String? = null
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val id: Int? = null,
    val nombre: String? = null,
    val nombre_usuario: String? = null,
    val correo: String? = null
)

// 2. La interfaz de Retrofit que define el endpoint
interface ApiService {
    @FormUrlEncoded
    @POST("/api/auth/login/local")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @GET("/api/auth/usuarios/buscar")
    suspend fun buscarUsuario(@Query("correo") correo: String): Response<UsuarioLogin>

    @Multipart
    @POST("/tickets/")
    suspend fun crearTicket(
        @Part("descripcion") descripcion: RequestBody,
        @Part("usuario_id") usuarioId: RequestBody,
        @Part("correo_solicitante") correo: RequestBody,
        @Part("sede") sede: RequestBody,
        @Part("piso") piso: RequestBody,
        @Part archivo: MultipartBody.Part?
    ): Response<TicketResponse>

    @GET("videos/")
    suspend fun obtenerVideosTutoriales(@Query("tags") tags: String? = null): Response<List<VideoTutorial>>

    @POST("tickets/{ticket_id}/resolver-autoatencion")
    suspend fun resolverTicketAutoatencion(@Path("ticket_id") ticketId: Int): Response<Unit>

    @GET("tickets/usuario/{usuario_id}")
    suspend fun obtenerMisSolicitudes(@Path("usuario_id") usuarioId: Int): Response<List<TicketRespuesta>>

    @PATCH("/api/auth/fcm-token")
    suspend fun actualizarFcmToken(@Body fcmTokenRequest: FcmTokenRequest): Response<Unit>
}

data class FcmTokenRequest(
    val token: String
)

// 3. El cliente configurado para el emulador
object RetrofitClient {
    private const val BASE_URL = "https://helpstream-api.onrender.com/"
    private var apiService: ApiService? = null

    fun getInstance(context: Context): ApiService {
        if (apiService == null) {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val sharedPref = context.getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
                    val token = sharedPref.getString("JWT_TOKEN", "") ?: ""
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}
