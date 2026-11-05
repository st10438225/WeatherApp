package com.example.weatherapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchActivity : ComponentActivity() {

    private lateinit var adapter: SearchAdapter
    private var searchResults = listOf<GeoResult>()
    private var favouriteCities = listOf<FavouriteCity>()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private lateinit var dao: FavouriteCityDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        dao = AppDatabase.getDatabase(this).favouriteCityDao()

        findViewById<ImageView>(R.id.btnBackSearch).setOnClickListener { finish() }
        val editSearch = findViewById<EditText>(R.id.editSearch)
        val recyclerView = findViewById<RecyclerView>(R.id.rvSearchResults)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SearchAdapter(
            results = searchResults,
            favouriteCities = favouriteCities,
            onCityClick = { city ->
                val intent = Intent()
                intent.putExtra("lat", city.lat)
                intent.putExtra("lon", city.lon)
                intent.putExtra("cityName", city.name)
                setResult(RESULT_OK, intent)
                finish()
            },
            onStarClick = { city, isFav -> toggleFavourite(city, isFav) }
        )
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            dao.observeAll().collectLatest { favs ->
                favouriteCities = favs
                adapter.updateFavourites(favs) // Call a method to update the list
            }
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
            }
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length > 2) {
                    searchRunnable = Runnable { performSearch(query) }
                    handler.postDelayed(searchRunnable!!, 400)
                } else {
                    searchResults = emptyList()
                    adapter.updateResults(emptyList())
                }
            }
        })
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            try {
                searchResults = RetrofitClient.weatherApi.getCitySuggestions(query, 5, Constants.APP_ID)
                adapter.updateResults(searchResults)
            } catch (e: Exception) {
                Log.e("SEARCH", "Failed to fetch cities", e)
                Toast.makeText(this@SearchActivity, "City not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFavourite(city: GeoResult, isCurrentlyFav: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isCurrentlyFav) {
                dao.delete(city.name)
            } else {
                dao.insert(
                    FavouriteCity(
                        cityName = city.name,
                        country = city.country,
                        lat = city.lat,
                        lon = city.lon,
                        lastKnownTempC = 0.0
                    )
                )
            }
        }
    }
}