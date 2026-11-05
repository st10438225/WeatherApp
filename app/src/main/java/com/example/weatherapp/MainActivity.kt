package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.EventsRepository
import com.example.weatherapp.utils.SettingsRepository
import com.example.weatherapp.utils.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private val REQUEST_LOCATION_CODE = 123321
    private var tempUnit: String = "C"

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var repository: WeatherRepository
    private val eventsRepository = EventsRepository()
    private lateinit var eventsAdapter: EventsAdapter

    private lateinit var weatherCache: LastWeatherCache
    private lateinit var db: AppDatabase
    private lateinit var dao: FavouriteCityDao
    private var lastFetchedWeather: WeatherResponse? = null
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Init Components
        repository = WeatherRepository(this)
        weatherCache = LastWeatherCache(this)
        db = AppDatabase.getDatabase(this)
        dao = db.favouriteCityDao()
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        lifecycleScope.launch {
            val settings = SettingsRepository.load()
            if (settings != null) {
                tempUnit = settings.tempUnit

            }
        }

        setupViews()
        observeFavourites()
        checkLocationAndPermissions()
    }

    private fun setupViews() {
        //Events RecyclerView
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
            if (currentLat != 0.0 && currentLon != 0.0) {
                val intent = Intent(this, ForecastActivity::class.java)
                intent.putExtra("lat", currentLat)
                intent.putExtra("lon", currentLon)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Weather data not loaded yet", Toast.LENGTH_SHORT).show()
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 999)
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

    private fun checkLocationAndPermissions() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location is not turned on", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            requestPermission()
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            val result = repository.getWeatherByLocation(latitude, longitude)
            result.onSuccess { weatherResult ->
                setupUI(weatherResult.data)

                //LIVE/OFFLINE Badge
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
                Log.e(Constants.LOG_TAG, "Failed to load events", it)            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        lifecycleScope.launch {
            try {
                val location = mFusedLocationProviderClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                if (location != null) {
                    getLocationWeatherDetails(location.latitude, location.longitude)
                    getEventsNearby(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this@MainActivity, "Could not get location. Is GPS on?", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Error getting location", e)
                Toast.makeText(this@MainActivity, "Failed to get location", Toast.LENGTH_SHORT).show()
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

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showRequestDialog()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            showRequestDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_LOCATION_CODE) return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationData()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRequestDialog() {
        AlertDialog.Builder(this)
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("CLOSE") { dialog, _ ->
                dialog.cancel()
            }.setTitle("Location permission needed")
            .setMessage("This permission is needed to access the location, enable under Application settings.")
            .show()
    }
}
