package com.example.helpstream_mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
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
    private val coroutineScope: CoroutineScope
) : RecyclerView.Adapter<VideoTutorialAdapter.VideoViewHolder>() {

    private val activePlayers = mutableListOf<ExoPlayer>()

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.player_view)
        val textTitulo: TextView = view.findViewById(R.id.text_video_titulo)
        val textDescripcion: TextView = view.findViewById(R.id.text_video_descripcion)
        val btnSolucione: LinearLayout = view.findViewById(R.id.btnSolucioneProblema)
        val btnNoFunciono: LinearLayout = view.findViewById(R.id.btnNoFunciono)
        val btnCompartir: LinearLayout = view.findViewById(R.id.btnCompartir)
        val chipGroupTags: ChipGroup = view.findViewById(R.id.chip_group_tags)
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

        // Setup Chips
        holder.chipGroupTags.removeAllViews()
        val tagsArray = video.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        for (tag in tagsArray) {
            val chip = Chip(context).apply {
                text = if (tag.startsWith("#")) tag else "#$tag"
                setChipBackgroundColorResource(android.R.color.transparent)
                chipStrokeWidth = 1f
                setChipStrokeColorResource(android.R.color.white)
                setTextColor(context.getColor(android.R.color.white))
                isCheckable = false
            }
            holder.chipGroupTags.addView(chip)
        }

        // Inicializar ExoPlayer y registrarlo
        val player = ExoPlayer.Builder(context).build()
        activePlayers.add(player)
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

        // Lógica de botones
        holder.btnSolucione.setOnClickListener {
            resolverTicket()
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

    private fun resolverTicket() {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getInstance(context).resolverTicketAutoatencion(ticketId)
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "¡Excelente! Ticket atendido y cerrado automáticamente", Toast.LENGTH_LONG).show()
                    (context as? Activity)?.finish()
                } else {
                    Toast.makeText(context, "Error al resolver ticket: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.player?.let {
            activePlayers.remove(it)
            it.release()
        }
        holder.player = null
        holder.playerView.player = null
    }

    fun pauseAllPlayers() {
        activePlayers.forEach { it.pause() }
    }

    fun releaseAllPlayers() {
        activePlayers.forEach { it.release() }
        activePlayers.clear()
    }

    override fun getItemCount(): Int = videoList.size

    fun updateData(newList: List<VideoTutorial>) {
        videoList = newList
        notifyDataSetChanged()
    }
}