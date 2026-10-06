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
