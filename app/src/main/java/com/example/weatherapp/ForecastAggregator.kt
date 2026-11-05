package com.example.weatherapp

data class DailyForecast(
    val date: String,
    val dayOfWeek: String,
    val minTemp: Double,
    val maxTemp: Double,
    val description: String,
    val icon: String
)

object ForecastAggregator {
    fun aggregate(items: List<ForecastItem>): List<DailyForecast> {
        // Group by the date part of "yyyy-MM-dd HH:mm:ss"
        val groupedByDay = items.groupBy { it.dtTxt.split(" ")[0] }

        return groupedByDay.map { (date, dayItems) ->
            val minTemp = dayItems.minOf { it.weatherMain.temp_min }
            val maxTemp = dayItems.maxOf { it.weatherMain.temp_max }

            // Find the most frequent weather description for that day
            val dominantWeather = dayItems
                .groupingBy { it.weather.firstOrNull()?.description ?: "Unknown" }
                .eachCount()
                .maxByOrNull { it.value }

            val desc = dominantWeather?.key ?: "Clear"

            // Get the icon corresponding to that description
            val icon = dayItems.firstOrNull { it.weather.firstOrNull()?.description == desc }
                ?.weather?.firstOrNull()?.icon ?: "01d"

            // Extract day of week (Mon, Tue, etc.)
            val dayOfWeek = try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dateObj = format.parse(date)
                val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                dayFormat.format(dateObj ?: java.util.Date())
            } catch (e: Exception) {
                ""
            }

            DailyForecast(date, dayOfWeek, minTemp, maxTemp, desc, icon)
        }.take(5)
    }
}