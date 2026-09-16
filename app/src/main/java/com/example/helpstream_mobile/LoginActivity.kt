package com.example.helpstream_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Verificar si ya había iniciado sesión antes (Persistencia de sesión)
        val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
        val usuarioLogueado = sharedPref.contains("USER_ID")

        if (usuarioLogueado) {
            irAPantallaPrincipal()
            return
        }

        setContentView(R.layout.activity_login)

        val btnLoginSSO = findViewById<Button>(R.id.btnLoginSSO)

        btnLoginSSO.setOnClickListener {
            Toast.makeText(this, "Conectando con Microsoft Entra ID...", Toast.LENGTH_SHORT).show()

            // Simulamos la respuesta exitosa del token de Microsoft 365
            // Guardamos el ID del operario y su nombre corporativo en la memoria del celular
            with (sharedPref.edit()) {
                putInt("USER_ID", 101)
                putString("USER_NAME", "Juan Pérez (Operario)")
                apply()
            }

            Toast.makeText(this, "¡Autenticación exitosa!", Toast.LENGTH_SHORT).show()
            irAPantallaPrincipal()
        }
    }

    private fun irAPantallaPrincipal() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // Cerramos el login para que si le da atrás no vuelva a pedir login
    }
}