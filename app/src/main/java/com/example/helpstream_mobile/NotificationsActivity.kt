package com.example.helpstream_mobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

        // Mock list of notifications
        val mockNotifications = listOf(
            NotificationItem("Ticket #70 actualizado", "El estado de tu ticket ha cambiado a EN PROCESO"),
            NotificationItem("Ticket #71 actualizado", "El estado de tu ticket ha cambiado a RESUELTO")
        )

        rvNotifications.adapter = NotificationsAdapter(mockNotifications)
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