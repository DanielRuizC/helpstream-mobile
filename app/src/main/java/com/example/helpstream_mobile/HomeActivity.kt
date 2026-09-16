package com.example.helpstream_mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
}
