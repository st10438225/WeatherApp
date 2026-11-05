package com.example.weatherapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastAggregatorTest {


    private fun createItem(dateTime: String, minTemp: Double = 10.0, maxTemp: Double = 20.0, desc: String = "Clouds"): ForecastItem {
        return ForecastItem(
            dt = 0L,
            weatherMain = WeatherMain(
                temp = 15.0,
                feels_like = 15.0,
                temp_min = minTemp,
                temp_max = maxTemp,
                pressure = 1000,
                humidity = 50
            ),
            weather = listOf(Weather(id = 800, main = "Clear", description = desc, icon = "01d")),
            dtTxt = dateTime
        )
    }

    @Test
    fun `empty list returns empty`() {
        val result = ForecastAggregator.aggregate(emptyList())
        assertEquals(0, result.size)
    }

    @Test
    fun `single day returns 1 entry`() {
        val items = listOf(
            createItem("2024-05-20 09:00:00", minTemp = 10.0, maxTemp = 15.0),
            createItem("2024-05-20 12:00:00", minTemp = 12.0, maxTemp = 18.0),
            createItem("2024-05-20 15:00:00", minTemp = 11.0, maxTemp = 20.0)
        )
        val result = ForecastAggregator.aggregate(items)

        assertEquals(1, result.size)
        assertEquals("2024-05-20", result[0].date)
        assertEquals(10.0, result[0].minTemp, 0.0) // Lowest of the day
        assertEquals(20.0, result[0].maxTemp, 0.0) // Highest of the day
    }

    @Test
    fun `crosses midnight returns 2 entries`() {
        val items = listOf(
            createItem("2024-05-20 23:00:00"),
            createItem("2024-05-21 02:00:00")
        )
        val result = ForecastAggregator.aggregate(items)
        assertEquals(2, result.size)
        assertEquals("2024-05-20", result[0].date)
        assertEquals("2024-05-21", result[1].date)
    }

    @Test
    fun `more than 5 days is capped at 5`() {
        val items = listOf(
            createItem("2024-05-20 12:00:00"),
            createItem("2024-05-21 12:00:00"),
            createItem("2024-05-22 12:00:00"),
            createItem("2024-05-23 12:00:00"),
            createItem("2024-05-24 12:00:00"),
            createItem("2024-05-25 12:00:00") // 6th day
        )
        val result = ForecastAggregator.aggregate(items)
        assertEquals(5, result.size) // Should drop the 6th day
    }
}