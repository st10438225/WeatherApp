package com.example.weatherapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ForecastActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forecast)

        findViewById<ImageView>(R.id.btnBackForecast).setOnClickListener { finish() }

        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)

        if (lat == 0.0 && lon == 0.0) {
            Toast.makeText(this, "Location not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchForecast(lat, lon)
    }

    private fun fetchForecast(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {

                val response = RetrofitClient.weatherApi.getForecast(lat, lon, Constants.APP_ID, Constants.METRIC_UNIT)

                if (response.list != null) {
                    val dailyForecasts = ForecastAggregator.aggregate(response.list)
                    val recyclerView = findViewById<RecyclerView>(R.id.rvForecast)
                    recyclerView.layoutManager = LinearLayoutManager(this@ForecastActivity)
                    recyclerView.adapter = ForecastAdapter(dailyForecasts)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForecastActivity, "Failed to load forecast", Toast.LENGTH_SHORT).show()
            }
        }
    }
}