package com.example.helpstream_mobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoatencionActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var searchEditText: TextInputEditText
    private lateinit var searchLayout: TextInputLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnSearch: ImageView
    private var adapter: VideoTutorialAdapter? = null
    private var allVideos = listOf<VideoTutorial>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_autoatencion)

        viewPager = findViewById(R.id.video_view_pager)
        searchEditText = findViewById(R.id.search_edit_text)
        searchLayout = findViewById(R.id.search_layout)
        btnBack = findViewById(R.id.btn_back)
        btnSearch = findViewById(R.id.btn_search)

        val ticketId = intent.getIntExtra("TICKET_ID", -1)

        adapter = VideoTutorialAdapter(this, emptyList(), "helpstream-api.onrender.com", ticketId, lifecycleScope)
        viewPager.adapter = adapter

        setupSearch()
        setupTopBar()

        val filterTags = intent.getStringExtra("FILTER_TAGS")
        cargarVideos(filterTags)
    }

    private fun setupTopBar() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnSearch.setOnClickListener {
            if (searchLayout.visibility == View.VISIBLE) {
                searchLayout.visibility = View.GONE
            } else {
                searchLayout.visibility = View.VISIBLE
                searchEditText.requestFocus()
            }
        }
    }

    private fun cargarVideos(tags: String? = null) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getInstance(this@AutoatencionActivity).obtenerVideosTutoriales(tags)
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