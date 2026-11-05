package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.EventsRepository
import com.example.weatherapp.utils.SettingsRepository
import com.example.weatherapp.utils.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private var tempUnit: String = "C"
    private lateinit var repository: WeatherRepository
    private val eventsRepository = EventsRepository()
    private lateinit var eventsAdapter: EventsAdapter

    private lateinit var db: AppDatabase
    private lateinit var dao: FavouriteCityDao
    private var lastFetchedWeather: WeatherResponse? = null

    // Default fallback: Johannesburg
    private var currentLat: Double = -26.2041
    private var currentLon: Double = 28.0473

    private val searchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val lat = result.data!!.getDoubleExtra("lat", 0.0)
            val lon = result.data!!.getDoubleExtra("lon", 0.0)
            val cityName = result.data!!.getStringExtra("cityName") ?: ""

            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit().putString("last_city", cityName).apply()

            getLocationWeatherDetails(lat, lon)
            getEventsNearby(lat, lon)
            findViewById<TextView>(R.id.tvLocation).text = cityName
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            requestLocationData()
        } else {
            Toast.makeText(this, "Location denied. Showing default city.", Toast.LENGTH_SHORT).show()
            getLocationWeatherDetails(currentLat, currentLon)
            getEventsNearby(currentLat, currentLon)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = WeatherRepository(this)
        db = AppDatabase.getDatabase(this)
        dao = db.favouriteCityDao()

        lifecycleScope.launch {
            val settings = SettingsRepository.load()
            if (settings != null) {
                tempUnit = settings.tempUnit
            }
        }

        setupViews()
        observeFavourites()

        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationData()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        lifecycleScope.launch {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)

                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                } else {
                    Toast.makeText(this@MainActivity, "GPS failed. Showing default city.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Location error", e)
                Toast.makeText(this@MainActivity, "Location error. Showing default city.", Toast.LENGTH_SHORT).show()
            } finally {
                getLocationWeatherDetails(currentLat, currentLon)
                getEventsNearby(currentLat, currentLon)
            }
        }
    }

    private fun setupViews() {
        val rvEvents = findViewById<RecyclerView>(R.id.rvEvents)
        rvEvents.layoutManager = LinearLayoutManager(this)
        eventsAdapter = EventsAdapter(emptyList())
        rvEvents.adapter = eventsAdapter

        findViewById<TextView>(R.id.btnOpenSearch).setOnClickListener {
            searchLauncher.launch(Intent(this, SearchActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewForecast).setOnClickListener {
            val intent = Intent(this, ForecastActivity::class.java)
            intent.putExtra("lat", currentLat)
            intent.putExtra("lon", currentLon)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnAddFavourite).setOnClickListener {
            lastFetchedWeather?.let { weather ->
                lifecycleScope.launch {
                    if (dao.isFavourite(weather.name)) {
                        dao.delete(weather.name)
                        Toast.makeText(this@MainActivity, "Removed from favourites", Toast.LENGTH_SHORT).show()
                    } else {
                        dao.insert(
                            FavouriteCity(
                                cityName = weather.name,
                                country = weather.sys.country,
                                lat = weather.coord.lat,
                                lon = weather.coord.lon,
                                lastKnownTempC = weather.weatherMain.temp
                            )
                        )
                        Toast.makeText(this@MainActivity, "Saved to favourites", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: Toast.makeText(this, "Weather not loaded yet", Toast.LENGTH_SHORT).show()
        }

        // Notification Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 999)
            }
        }
    }

    private fun observeFavourites() {
        lifecycleScope.launch {
            dao.observeAll().collectLatest { favourites ->
                renderFavourites(favourites)
            }
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            val result = repository.getWeatherByLocation(latitude, longitude)
            result.onSuccess { weatherResult ->
                setupUI(weatherResult.data)

                val tvStatus = findViewById<TextView>(R.id.tvStatus)
                if (weatherResult.isOffline) {
                    tvStatus.text = "OFFLINE"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#C0392B"))
                } else {
                    tvStatus.text = "LIVE"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#247A52"))
                }
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getEventsNearby(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            val result = eventsRepository.getEventsNearby(latitude, longitude)
            result.onSuccess { events ->
                eventsAdapter = EventsAdapter(events)
                findViewById<RecyclerView>(R.id.rvEvents).adapter = eventsAdapter
            }.onFailure {
                Log.e(Constants.LOG_TAG, "Failed to load events", it)
            }
        }
    }

    private fun setupUI(weatherResponse: WeatherResponse) {
        currentLat = weatherResponse.coord.lat
        currentLon = weatherResponse.coord.lon
        lastFetchedWeather = weatherResponse

        findViewById<TextView>(R.id.tvLocation).text = weatherResponse.name
        findViewById<TextView>(R.id.tvCountry).text = weatherResponse.sys.country
        findViewById<TextView>(R.id.tvDescription).text = weatherResponse.weather[0].description
        findViewById<TextView>(R.id.tvHumidity).text = "${weatherResponse.weatherMain.humidity}%"
        findViewById<TextView>(R.id.tvWind).text = "${weatherResponse.wind.speed} m/s"
        findViewById<TextView>(R.id.tvPressure).text = "${weatherResponse.weatherMain.pressure} hPa"
        findViewById<TextView>(R.id.tvVisibility).text = "${weatherResponse.visibility / 1000} km"
        findViewById<TextView>(R.id.tvCloudiness).text = "${weatherResponse.clouds.all}%"

        findViewById<TextView>(R.id.tvTemperature).text = fmtTemp(weatherResponse.weatherMain.temp)
        findViewById<TextView>(R.id.tvFeelsLike).text  = "Feels like ${fmtTemp(weatherResponse.weatherMain.feels_like)}"
        findViewById<TextView>(R.id.tvMinTemp).text    = fmtTemp(weatherResponse.weatherMain.temp_min)
        findViewById<TextView>(R.id.tvMaxTemp).text    = fmtTemp(weatherResponse.weatherMain.temp_max)
    }

    private fun renderFavourites(favourites: List<FavouriteCity>) {
        val container = findViewById<LinearLayout>(R.id.favouritesContainer)
        container.removeAllViews()

        if (favourites.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No favourite cities saved yet."
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                setPadding(0, 12, 0, 12)
            })
            return
        }

        favourites.forEach { fav ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(24, 24, 24, 24)
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.weather_card)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                isClickable = true
                setOnClickListener {
                    getLocationWeatherDetails(fav.lat, fav.lon)
                    getEventsNearby(fav.lat, fav.lon)
                }
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            textLayout.addView(TextView(this).apply {
                text = "${fav.cityName}, ${fav.country}"
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#172B4D"))
            })

            textLayout.addView(TextView(this).apply {
                text = "${"%.1f".format(fav.lastKnownTempC)}°C • Saved offline"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                setPadding(0, 4, 0, 0)
            })

            row.addView(textLayout)

            row.addView(TextView(this).apply {
                text = "✕"
                textSize = 18f
                setTextColor(android.graphics.Color.parseColor("#C0392B"))
                setPadding(24, 24, 24, 24)
                setOnClickListener {
                    lifecycleScope.launch {
                        dao.delete(fav.cityName)
                    }
                }
            })

            container.addView(row)
        }
    }

    private fun fmtTemp(celsius: Double): String {
        return if (tempUnit == "F") "%.1f°F".format(celsius * 9.0 / 5.0 + 32.0)
        else "%.1f°C".format(celsius)
    }
}