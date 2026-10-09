package com.example.helpstream_mobile

import android.Manifest
import android.content.Context
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

        val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
        val userNameRaw = sharedPref.getString("USER_NAME", "")
        
        if (userNameRaw.isNullOrEmpty() || userNameRaw == "null" || userNameRaw == "null null") {
            sharedPref.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        val tvWelcome = findViewById<android.widget.TextView>(R.id.tvWelcomeName)
        val spanString = android.text.SpannableString("Hola, $userNameRaw")
        spanString.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 6, spanString.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvWelcome.text = spanString
        
        val initials = userNameRaw.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
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
