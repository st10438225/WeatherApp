package com.example.weatherapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.weatherapp.BuildConfig

object Constants
{
     val APP_ID = BuildConfig.OPENWEATHER_API_KEY
     val TICKETMASTER_API_KEY = BuildConfig.TICKETMASTER_API_KEY

    const val BASE_URL = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT = "metric"
    const val TICKETMASTER_BASE_URL = "https://app.ticketmaster.com/discovery/v2/"

    const val LOG_TAG = "WeatherApp"

    fun isNetworkAvailable(context: Context): Boolean
    {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M)
        {
            val network = connectivityManager.activeNetwork?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network)?: return false
            return when
            {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)-> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> return true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)-> return true
                else->return false
            }
        }
        else
        {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}
