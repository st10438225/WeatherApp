package com.example.weatherapp.utils

object GeoHash {
    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(latitude: Double, longitude: Double, precision: Int = 6): String {
        var latMin = -90.0
        var latMax = 90.0
        var lonMin = -180.0
        var lonMax = 180.0
        val hash = StringBuilder()
        var bit = 0
        var ch = 0
        var even = true

        while (hash.length < precision) {
            if (even) {
                val mid = (lonMin + lonMax) / 2
                if (longitude >= mid) { ch = ch or (1 shl (4 - bit)); lonMin = mid }
                else lonMax = mid
            } else {
                val mid = (latMin + latMax) / 2
                if (latitude >= mid) { ch = ch or (1 shl (4 - bit)); latMin = mid }
                else latMax = mid
            }
            even = !even
            if (bit < 4) bit++ else {
                hash.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return hash.toString()
    }
}