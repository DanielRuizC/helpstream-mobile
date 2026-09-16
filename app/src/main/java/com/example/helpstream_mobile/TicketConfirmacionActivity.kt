package com.example.helpstream_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

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

        findViewById<MaterialButton>(R.id.btnVideosSugeridos).setOnClickListener {
            val intent = Intent(this, AutoatencionActivity::class.java)
            intent.putExtra("TICKET_ID", ticketId)
            intent.putExtra("FILTER_TAGS", tags.joinToString(","))
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btnVolverInicio).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}