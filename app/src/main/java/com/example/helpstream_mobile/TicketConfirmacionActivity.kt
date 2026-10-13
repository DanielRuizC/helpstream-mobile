package com.example.helpstream_mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketConfirmacionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_confirmacion)

        val ticketId = intent.getIntExtra("TICKET_ID", 0)
        val fecha = intent.getStringExtra("FECHA") ?: ""
        val descripcion = intent.getStringExtra("DESCRIPCION") ?: ""
        val tags = intent.getStringArrayListExtra("TAGS") ?: arrayListOf()

        findViewById<TextView>(R.id.tvTicketNumberValue).text = "#$ticketId"
        findViewById<TextView>(R.id.tvDateTimeValue).text = fecha
        findViewById<TextView>(R.id.tvDetailsValue).text = descripcion

        val chipGroup = findViewById<ChipGroup>(R.id.cgTags)
        chipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(this)
            chip.text = tag
            chip.setChipBackgroundColorResource(R.color.corporate_blue)
            chip.setTextColor(getColor(R.color.white))
            chipGroup.addView(chip)
        }

        val btnVideosSugeridos = findViewById<MaterialButton>(R.id.btnVideosSugeridos)
        val tvBtnSubtitle = findViewById<TextView>(R.id.tvBtnSubtitle)

        // Por defecto, ocultamos el botón de videos sugeridos y su subtítulo
        btnVideosSugeridos.visibility = View.GONE
        tvBtnSubtitle.visibility = View.GONE

        btnVideosSugeridos.setOnClickListener {
            val intent = Intent(this, AutoatencionActivity::class.java)
            intent.putExtra("TICKET_ID", ticketId)
            intent.putExtra("FILTER_TAGS", tags.joinToString(","))
            intent.putExtra("from_ticket", true)
            startActivity(intent)
        }

        // Validación de resultados antes de mostrar el botón
        if (tags.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val api = RetrofitClient.getInstance(this@TicketConfirmacionActivity)
                    val response = api.obtenerVideosTutoriales(tags.joinToString(","))
                    
                    if (response.isSuccessful) {
                        val videos = response.body()
                        if (!videos.isNullOrEmpty()) {
                            withContext(Dispatchers.Main) {
                                btnVideosSugeridos.visibility = View.VISIBLE
                                tvBtnSubtitle.visibility = View.VISIBLE
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnVolverInicio).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}