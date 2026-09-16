package com.example.helpstream_mobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoatencionActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var searchEditText: TextInputEditText
    private var adapter: VideoTutorialAdapter? = null
    private var allVideos = listOf<VideoTutorial>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_autoatencion)

        viewPager = findViewById(R.id.video_view_pager)
        searchEditText = findViewById(R.id.search_edit_text)

        val ticketId = intent.getIntExtra("TICKET_ID", -1)

        // Inicializar el adaptador con ticketId y lifecycleScope
        adapter = VideoTutorialAdapter(this, emptyList(), "10.0.2.2:8000", ticketId, lifecycleScope)
        viewPager.adapter = adapter

        setupSearch()

        val filterTags = intent.getStringExtra("FILTER_TAGS")
        cargarVideos(filterTags)
    }

    private fun cargarVideos(tags: String? = null) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.obtenerVideosTutoriales(tags)
                }

                if (response.isSuccessful) {
                    allVideos = response.body() ?: emptyList()
                    adapter?.updateData(allVideos)
                } else {
                    Toast.makeText(this@AutoatencionActivity, "Error al cargar videos: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AutoatencionActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVideos(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterVideos(query: String) {
        val filteredList = if (query.isEmpty()) {
            allVideos
        } else {
            allVideos.filter {
                it.titulo.contains(query, ignoreCase = true) || 
                it.tags.contains(query, ignoreCase = true)
            }
        }
        adapter?.updateData(filteredList)
    }

    override fun onPause() {
        super.onPause()
        adapter?.pauseAllPlayers()
    }

    override fun onStop() {
        super.onStop()
        adapter?.releaseAllPlayers()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.releaseAllPlayers()
    }
}