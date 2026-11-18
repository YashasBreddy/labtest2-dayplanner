package com.example.dayplanner.models

data class RouteResult(
    val orderedPlaces: List<Place>,
    val polylinePoints: List<Pair<Double, Double>>, // Lat, Lng pairs
    val totalDistanceKm: Double,
    val totalDurationMinutes: Int,
    val summary: String,
    val isOptimized: Boolean = true
)
