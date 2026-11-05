package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class TicketmasterResponse(
    @SerializedName("_embedded") val embedded: TMEmbedded?
)

data class TMEmbedded(
    val events: List<TMEvent>?
)

data class TMEvent(
    val name: String?,
    val url: String?,
    val distance: Double?,
    val units: String?,
    val dates: TMDates?,
    val classifications: List<TMClassification>?,
    @SerializedName("_embedded") val embedded: TMEventEmbedded?
)

data class TMDates(val start: TMStart?)
data class TMStart(val localDate: String?, val localTime: String?)

data class TMClassification(
    val primary: Boolean?,
    val segment: TMSegment?
)
data class TMSegment(val name: String?)

data class TMEventEmbedded(
    val venues: List<TMVenue>?
)

data class TMVenue(
    val name: String?,
    val city: TMCity?
)
data class TMCity(val name: String?)