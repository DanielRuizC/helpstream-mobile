package com.example.helpstream_mobile

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
        val btnSolucione: MaterialButton = view.findViewById(R.id.btnSolucioneProblema)
        val btnNoFunciono: MaterialButton = view.findViewById(R.id.btnNoFunciono)
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
            "http://$backendIp/$cleanUrl"
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
    }

    private fun resolverTicket() {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.resolverTicketAutoatencion(ticketId)
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