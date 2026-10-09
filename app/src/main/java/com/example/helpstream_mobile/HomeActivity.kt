package com.example.helpstream_mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    private val NOTIFICATION_PERMISSION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        solicitarPermisoNotificaciones()

        findViewById<MaterialCardView>(R.id.cardRegistrar).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardMisSolicitudes).setOnClickListener {
            startActivity(Intent(this, MisSolicitudesActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardVerVideos).setOnClickListener {
            val intent = Intent(this, AutoatencionActivity::class.java)
            intent.putExtra("FILTER_TAGS", "")
            startActivity(intent)
        }

        val sharedPref = getSharedPreferences("HelpStreamSession", android.content.Context.MODE_PRIVATE)
        val userName = sharedPref.getString("USER_NAME", "Usuario") ?: "Usuario"
        
        findViewById<android.widget.TextView>(R.id.tvWelcomeName).text = "Hola, $userName"
        
        val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        findViewById<android.widget.TextView>(R.id.tvInitials).text = if (initials.isNotEmpty()) initials else "U"

        findViewById<android.widget.ImageButton>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<android.widget.ImageButton>(R.id.btnLogout).setOnClickListener {
            sharedPref.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
}
