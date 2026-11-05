package com.example.weatherapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventsAdapter(private val events: List<TMEvent>) :
    RecyclerView.Adapter<EventsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEventName: TextView = view.findViewById(R.id.tvEventName)
        val tvEventVenue: TextView = view.findViewById(R.id.tvEventVenue)
        val tvEventDate: TextView = view.findViewById(R.id.tvEventDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]

        holder.tvEventName.text = event.name ?: "Untitled event"

        val venue = event.embedded?.venues?.firstOrNull()?.name
        val city = event.embedded?.venues?.firstOrNull()?.city?.name
        holder.tvEventVenue.text = listOfNotNull(venue, city).joinToString(", ")

        holder.tvEventDate.text = event.dates?.start?.localDate ?: "Date TBA"

        holder.itemView.setOnClickListener {
            val url = event.url
            if (!url.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = events.size
}