package com.example.helpstream_mobile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    data class NotificationItem(val title: String, val body: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val toolbar = findViewById<Toolbar>(R.id.toolbarNotifications)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val rvNotifications = findViewById<RecyclerView>(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        val sharedPref = getSharedPreferences("HelpStreamSession", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("USER_ID", -1)

        if (userId != -1) {
            cargarNotificacionesReales(userId, rvNotifications)
        } else {
            Toast.makeText(this, "Error: Sesión no encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarNotificacionesReales(userId: Int, recyclerView: RecyclerView) {
        lifecycleScope.launch {
            try {
                // Usamos el endpoint existente de obtener mis solicitudes para ver los últimos estados
                val response = RetrofitClient.getInstance(this@NotificationsActivity).obtenerMisSolicitudes(userId)
                
                if (response.isSuccessful) {
                    val tickets = response.body() ?: emptyList()
                    
                    if (tickets.isEmpty()) {
                        // En caso de que la respuesta sea 0 elementos, lo manejamos mostrando un texto vacío
                        // Aquí podríamos ocultar el RecyclerView y mostrar un TextView, pero para simplicidad 
                        // enviaremos un item especial informativo al Adapter.
                        recyclerView.adapter = NotificationsAdapter(listOf(
                            NotificationItem("No hay notificaciones nuevas", "Cuando actualicen tus tickets, aparecerán aquí.")
                        ))
                    } else {
                        // Mapeamos los tickets reales a elementos de notificación visual
                        val notificacionesReales = tickets.map { ticket ->
                            NotificationItem(
                                "Ticket #${ticket.id} actualizado",
                                "El estado de tu ticket ha cambiado a ${ticket.estado.uppercase()}"
                            )
                        }
                        recyclerView.adapter = NotificationsAdapter(notificacionesReales)
                    }
                } else {
                    Toast.makeText(this@NotificationsActivity, "Error al cargar el historial", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class NotificationsAdapter(private val notifications: List<NotificationItem>) :
        RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvNotificationTitle)
            val tvBody: TextView = view.findViewById(R.id.tvNotificationBody)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notif = notifications[position]
            holder.tvTitle.text = notif.title
            holder.tvBody.text = notif.body
        }

        override fun getItemCount() = notifications.size
    }
}