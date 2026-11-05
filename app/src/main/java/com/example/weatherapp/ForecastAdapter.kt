package com.example.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ForecastAdapter(private val forecasts: List<DailyForecast>) :
    RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvTemps: TextView = view.findViewById(R.id.tvTemps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecast = forecasts[position]
        holder.tvDay.text = forecast.dayOfWeek
        holder.tvDate.text = forecast.date
        holder.tvDesc.text = forecast.description.replaceFirstChar { it.uppercase() }
        holder.tvTemps.text = "${forecast.minTemp.toInt()}° / ${forecast.maxTemp.toInt()}°"
    }

    override fun getItemCount() = forecasts.size
}