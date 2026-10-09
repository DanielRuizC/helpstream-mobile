package com.example.helpstream_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
        val usuarioLogueado = sharedPref.contains("USER_ID")

        if (usuarioLogueado) {
            irAPantallaPrincipal()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tus credenciales", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            realizarLogin(email, password, sharedPref)
        }
    }

    private fun realizarLogin(email: String, password: String, sharedPref: android.content.SharedPreferences) {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Solicitamos el token
                val responseLogin = RetrofitClient.getInstance(this@LoginActivity).login(email, password)

                if (responseLogin.isSuccessful && responseLogin.body() != null) {
                    val loginData = responseLogin.body()!!

                    // Guardamos INMEDIATAMENTE el token para que el Interceptor de OkHttp pueda usarlo
                    with(sharedPref.edit()) {
                        putString("JWT_TOKEN", loginData.access_token)
                        apply()
                    }

                    // 2. Encadenamos la petición para buscar los datos del usuario por su correo
                    val responseUsuario = RetrofitClient.getInstance(this@LoginActivity).buscarUsuario(email)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true

                        if (responseUsuario.isSuccessful && responseUsuario.body() != null) {
                            try {
                                val usuario = responseUsuario.body()!!

                                val nombreLimpio = usuario.nombre ?: usuario.nombre_usuario ?: "Usuario"
                                val apellidosLimpio = usuario.apellidos ?: ""
                                val correoLimpio = usuario.correo ?: ""
                                val rolLimpio = usuario.rol_id ?: 0
                                
                                // Guardamos los datos del usuario en SharedPreferences garantizando no guardar strings "null"
                                with(sharedPref.edit()) {
                                    putInt("USER_ID", usuario.id)
                                    putString("USER_NAME", "$nombreLimpio $apellidosLimpio".trim())
                                    putString("USER_EMAIL", correoLimpio)
                                    putInt("USER_ROLE", rolLimpio)
                                    apply()
                                }

                                Toast.makeText(this@LoginActivity, "¡Bienvenido $nombreLimpio!", Toast.LENGTH_SHORT).show()
                                obtenerFCMToken()
                                irAPantallaPrincipal()
                            } catch (e: Exception) {
                                android.util.Log.e("LoginActivity", "Error procesando el inicio de sesión", e)
                                Toast.makeText(this@LoginActivity, "Error procesando el inicio de sesión", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorBody = responseUsuario.errorBody()?.string() ?: "Sin detalles al buscar usuario"
                            Toast.makeText(this@LoginActivity, "Error obteniendo datos: $errorBody", Toast.LENGTH_LONG).show()
                        }
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        val errorBody = responseLogin.errorBody()?.string() ?: "Sin detalles en el login"
                        Toast.makeText(this@LoginActivity, "Error Login ${responseLogin.code()}: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun obtenerFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.e("FCM", "Error al obtener el token FCM", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            if (token != null) {
                enviarFCMTokenAlBackend(token)
            }
        }
    }

    private fun enviarFCMTokenAlBackend(token: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = FcmTokenRequest(token)
                RetrofitClient.getInstance(this@LoginActivity).actualizarFcmToken(request)
                android.util.Log.d("FCM", "Token FCM actualizado en el backend exitosamente.")
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error al enviar el token FCM al backend", e)
                e.printStackTrace()
            }
        }
    }

    private fun irAPantallaPrincipal() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Cerramos el login para que si le da atrás no vuelva a pedir login
    }
}