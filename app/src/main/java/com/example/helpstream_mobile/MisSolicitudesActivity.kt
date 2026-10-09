package com.example.helpstream_mobile

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MisSolicitudesActivity : AppCompatActivity() {

    private lateinit var rvMisSolicitudes: RecyclerView
    private lateinit var adapter: MisSolicitudesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_solicitudes)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        rvMisSolicitudes = findViewById(R.id.rvMisSolicitudes)
        rvMisSolicitudes.layoutManager = LinearLayoutManager(this)

        val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("USER_ID", -1)

        val etSearch = findViewById<android.widget.EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (::adapter.isInitialized) {
                    adapter.filter(s.toString())
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        if (userId != -1) {
            cargarSolicitudes(userId)
        } else {
            Toast.makeText(this, "Error: Sesión no encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarSolicitudes(userId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@MisSolicitudesActivity).obtenerMisSolicitudes(userId)
                if (response.isSuccessful) {
                    val listado = response.body() ?: emptyList()
                    adapter = MisSolicitudesAdapter(listado)
                    rvMisSolicitudes.adapter = adapter
                } else {
                    Toast.makeText(this@MisSolicitudesActivity, "Error al cargar solicitudes", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisSolicitudesActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
