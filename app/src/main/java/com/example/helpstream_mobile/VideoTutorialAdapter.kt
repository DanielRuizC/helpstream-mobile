package com.example.helpstream_mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoTutorialAdapter(
    private val context: Context,
    private var videoList: List<VideoTutorial>,
    private val backendIp: String,
    private val ticketId: Int,
    private val fromTicket: Boolean,
    private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<VideoTutorialAdapter.VideoViewHolder>() {

    private val players = mutableMapOf<Int, ExoPlayer>()

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.player_view)
        val textTitulo: TextView = view.findViewById(R.id.text_video_titulo)
        val textDescripcion: TextView = view.findViewById(R.id.text_video_descripcion)
        val textVerMas: TextView = view.findViewById(R.id.text_ver_mas)
        val btnSolucione: LinearLayout = view.findViewById(R.id.btnSolucioneProblema)
        val btnNoFunciono: LinearLayout = view.findViewById(R.id.btnNoFunciono)
        val btnCompartir: LinearLayout = view.findViewById(R.id.btnCompartir)
        val chipGroupTags: ChipGroup = view.findViewById(R.id.chip_group_tags)
        val tvCasosResueltos: TextView = view.findViewById(R.id.tvCasosResueltos)
        var player: ExoPlayer? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_tutorial, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.textTitulo.text = video.titulo
        holder.textDescripcion.text = video.descripcion
        holder.tvCasosResueltos.text = "Ha ayudado a ${video.casos_resueltos} operarios"

        // Lógica "Ver más"
        var isExpanded = false
        holder.textVerMas.setOnClickListener {
            if (isExpanded) {
                holder.textDescripcion.maxLines = 3
                holder.textVerMas.text = "Ver más"
            } else {
                holder.textDescripcion.maxLines = Int.MAX_VALUE
                holder.textVerMas.text = "Ver menos"
            }
            isExpanded = !isExpanded
        }

        // Setup Chips
        holder.chipGroupTags.removeAllViews()
        val tagsArray = video.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        for (tag in tagsArray) {
            val chip = Chip(context).apply {
                text = if (tag.startsWith("#")) tag else "#$tag"
                
                // Configurar altura mínima reducida y tamaño de letra
                chipMinHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics)
                textSize = 11f
                
                // Fondo oscuro semitransparente sin bordes gruesos
                chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#99000000"))
                chipStrokeWidth = 0f
                
                // Texto en blanco puro
                setTextColor(Color.WHITE)
                isCheckable = false
            }
            holder.chipGroupTags.addView(chip)
        }

        // Inicializar ExoPlayer y registrarlo
        val player = ExoPlayer.Builder(context).build()
        players[position] = player
        holder.player = player
        holder.playerView.player = player

        // Concatenar IP del backend con url_video de forma segura
        val cleanUrl = video.url_video.removePrefix("/")
        val fullUrl = if (video.url_video.startsWith("http")) {
            video.url_video
        } else {
            "https://$backendIp/$cleanUrl"
        }

        val mediaItem = MediaItem.fromUri(fullUrl)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.prepare()
        player.playWhenReady = true

        // Visibilidad de botones contextuales
        if (fromTicket) {
            holder.btnSolucione.visibility = View.VISIBLE
            holder.btnNoFunciono.visibility = View.VISIBLE
        } else {
            holder.btnSolucione.visibility = View.GONE
            holder.btnNoFunciono.visibility = View.GONE
        }

        // Lógica de botones
        holder.btnSolucione.setOnClickListener {
            resolverTicket(video.id)
        }

        holder.btnNoFunciono.setOnClickListener {
            (context as? Activity)?.finish()
        }

        holder.btnCompartir.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Tutorial: ${video.titulo}")
                putExtra(Intent.EXTRA_TEXT, "Mira este tutorial: ${video.titulo}\n$fullUrl")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir tutorial"))
        }
    }

    private fun resolverTicket(videoId: Int) {
        coroutineScope.launch {
            try {
                // 1. Registrar resolución del video en las métricas
                withContext(Dispatchers.IO) {
                    RetrofitClient.getInstance(context).registrarResolucionVideo(videoId)
                }

                // 2. Si viene de un ticket, lo resolvemos también
                if (fromTicket) {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.getInstance(context).resolverTicketAutoatencion(ticketId)
                    }

                    if (response.isSuccessful) {
                        Toast.makeText(context, "¡Excelente! Ticket atendido y cerrado automáticamente", Toast.LENGTH_LONG).show()
                        (context as? Activity)?.finish()
                    } else {
                        Toast.makeText(context, "Error al resolver ticket: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "¡Gracias por tu feedback!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        val player = holder.player
        player?.let { p ->
            val entry = players.entries.find { it.value == p }
            if (entry != null) {
                players.remove(entry.key)
            }
            p.release()
        }
        holder.player = null
        holder.playerView.player = null
    }

    fun pauseAllPlayers() {
        players.values.forEach { it.pause() }
    }

    fun releaseAllPlayers() {
        players.values.forEach { it.release() }
        players.clear()
    }

    fun onPageSelected(position: Int) {
        players.forEach { (pos, player) ->
            if (pos == position) {
                player.play()
                
                // Registrar reproducción al iniciar el video a pantalla completa
                val video = videoList.getOrNull(pos)
                if (video != null) {
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                RetrofitClient.getInstance(context).registrarReproduccionVideo(video.id)
                            }
                        } catch (e: Exception) {
                            // Ignoramos el error para no interrumpir la reproducción
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                player.pause()
            }
        }
    }

    override fun getItemCount(): Int = videoList.size

    fun updateData(newList: List<VideoTutorial>) {
        videoList = newList
        notifyDataSetChanged()
    }
}