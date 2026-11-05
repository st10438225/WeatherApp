package com.example.weatherapp

import com.example.weatherapp.utils.GeoHash
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoHashTest {

    @Test
    fun `pretoria coordinates generate correct hash`() {
        val lat = -25.7461
        val lon = 28.1881

        val expectedHash = "ke7gkp"

        val actualHash = GeoHash.encode(lat, lon, precision = 6)
        assertEquals(expectedHash, actualHash)
    }

    @Test
    fun `cape town coordinates generate correct hash`() {
        val lat = -33.9249
        val lon = 18.4241

        val expectedHash = "k3vn5f"

        val actualHash = GeoHash.encode(lat, lon, precision = 6)
        assertEquals(expectedHash, actualHash)
    }
}