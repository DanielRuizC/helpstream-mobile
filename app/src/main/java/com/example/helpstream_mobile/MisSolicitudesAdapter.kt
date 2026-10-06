package com.example.helpstream_mobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MisSolicitudesAdapter(private val solicitudes: List<TicketRespuesta>) :
    RecyclerView.Adapter<MisSolicitudesAdapter.SolicitudViewHolder>() {

    class SolicitudViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTicketId: TextView = view.findViewById(R.id.tvTicketId)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvComentarioTecnico: TextView = view.findViewById(R.id.tvComentarioTecnico)
        val tvSede: TextView = view.findViewById(R.id.tvSede)
        val tvPiso: TextView = view.findViewById(R.id.tvPiso)
        val tvUsuarioNombre: TextView = view.findViewById(R.id.tvUsuarioNombre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitudViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud_ticket, parent, false)
        return SolicitudViewHolder(view)
    }

    override fun onBindViewHolder(holder: SolicitudViewHolder, position: Int) {
        val ticket = solicitudes[position]
        holder.tvTicketId.text = "Ticket #${ticket.id}"
        holder.tvFecha.text = ticket.fecha_creacion
        holder.tvDescripcion.text = ticket.descripcion

        holder.tvEstado.text = ticket.estado.uppercase()
        val color = when (ticket.estado.lowercase()) {
            "pendiente" -> "#FFC107" // Amarillo/Naranja
            "en proceso" -> "#2196F3" // Azul
            "atendido", "resuelto" -> "#2E7D32" // Verde oscuro
            else -> "#9E9E9E" // Gris
        }
        holder.tvEstado.background.mutate().setTint(Color.parseColor(color))

        holder.tvSede.text = "Sede: ${ticket.sede}"
        holder.tvPiso.text = "Piso: ${ticket.piso}"
        holder.tvUsuarioNombre.text = "👤 ${ticket.creador.nombre} ${ticket.creador.apellidos}"

        holder.tvUsuarioNombre.setOnClickListener {
            val context = holder.itemView.context
            android.app.AlertDialog.Builder(context)
                .setTitle("Contacto de ${ticket.creador.nombre}")
                .setMessage("Correo: ${ticket.creador.correo}\nTeléfono: ${ticket.creador.telefono}\nAnexo: ${ticket.creador.anexo}")
                .setPositiveButton("Cerrar", null)
                .show()
        }

        if (!ticket.comentario_tecnico.isNullOrBlank()) {
            holder.tvComentarioTecnico.text = "👨‍💻 Respuesta de TI: ${ticket.comentario_tecnico}"
        } else {
            holder.tvComentarioTecnico.text = "⏳ En espera de revisión por soporte TI"
        }
    }

    override fun getItemCount() = solicitudes.size
}
