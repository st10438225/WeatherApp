package com.example.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private var results: List<GeoResult>,
    private var favouriteCities: List<FavouriteCity>,
    private val onCityClick: (GeoResult) -> Unit,
    private val onStarClick: (GeoResult, Boolean) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCityName: TextView = view.findViewById(R.id.tvCityName)
        val tvCountryState: TextView = view.findViewById(R.id.tvCountryState)
        val btnStar: TextView = view.findViewById(R.id.btnStar)
    }

    fun updateResults(newResults: List<GeoResult>) {
        this.results = newResults
        notifyDataSetChanged()
    }

    fun updateFavourites(newFavourites: List<FavouriteCity>) {
        this.favouriteCities = newFavourites
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val city = results[position]
        holder.tvCityName.text = city.name
        holder.tvCountryState.text = listOfNotNull(city.country, city.state).joinToString(", ")

        val isFav = favouriteCities.any { it.cityName == city.name }
        holder.btnStar.text = if (isFav) "★" else "☆"

        holder.itemView.setOnClickListener { onCityClick(city) }
        holder.btnStar.setOnClickListener { onStarClick(city, isFav) }
    }

    override fun getItemCount() = results.size
}