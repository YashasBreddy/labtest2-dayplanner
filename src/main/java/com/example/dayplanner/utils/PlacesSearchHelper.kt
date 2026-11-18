package com.example.dayplanner.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.example.dayplanner.models.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

object PlacesSearchHelper {

    // Default curated sample places for quick testing or offline fallback
    val SAMPLE_SIGHTSEEING = listOf(
        Place(placeId = "sample_1", name = "Eiffel Tower", address = "Champ de Mars, 5 Av. Anatole France, Paris", latitude = 48.8584, longitude = 2.2945, stopType = Place.STOP_TYPE_SIGHTSEEING, rating = 4.8),
        Place(placeId = "sample_2", name = "Louvre Museum", address = "75001 Paris, France", latitude = 48.8606, longitude = 2.3376, stopType = Place.STOP_TYPE_SIGHTSEEING, rating = 4.7),
        Place(placeId = "sample_3", name = "Arc de Triomphe", address = "Place Charles de Gaulle, 75008 Paris", latitude = 48.8738, longitude = 2.2950, stopType = Place.STOP_TYPE_SIGHTSEEING, rating = 4.7),
        Place(placeId = "sample_4", name = "Sacré-Cœur Basilica", address = "35 Rue du Chevalier de la Barre, 75018 Paris", latitude = 48.8867, longitude = 2.3431, stopType = Place.STOP_TYPE_SIGHTSEEING, rating = 4.6),
        Place(placeId = "sample_5", name = "Notre-Dame Cathedral", address = "6 Parvis Notre-Dame, 75004 Paris", latitude = 48.8530, longitude = 2.3499, stopType = Place.STOP_TYPE_SIGHTSEEING, rating = 4.7)
    )

    val SAMPLE_FOOD_STOPS = listOf(
        Place(placeId = "food_1", name = "Le Bistro Parisien", address = "Port de la Bourdonnais, 75007 Paris", latitude = 48.8596, longitude = 2.2936, stopType = Place.STOP_TYPE_FOOD, rating = 4.5),
        Place(placeId = "food_2", name = "Café de Flore", address = "172 Blvd Saint-Germain, 75006 Paris", latitude = 48.8542, longitude = 2.3326, stopType = Place.STOP_TYPE_FOOD, rating = 4.4),
        Place(placeId = "food_3", name = "La Grande Crêperie", address = "12 Rue de Rivoli, 75004 Paris", latitude = 48.8556, longitude = 2.3550, stopType = Place.STOP_TYPE_FOOD, rating = 4.6),
        Place(placeId = "food_4", name = "Chez Jeannette", address = "47 Rue du Faubourg Saint-Denis, 75010 Paris", latitude = 48.8720, longitude = 2.3530, stopType = Place.STOP_TYPE_FOOD, rating = 4.3)
    )

    val SAMPLE_REST_STOPS = listOf(
        Place(placeId = "rest_1", name = "Jardin du Luxembourg", address = "75006 Paris, France", latitude = 48.8462, longitude = 2.3372, stopType = Place.STOP_TYPE_REST, rating = 4.8),
        Place(placeId = "rest_2", name = "Tuileries Garden", address = "Place de la Concorde, 75001 Paris", latitude = 48.8635, longitude = 2.3275, stopType = Place.STOP_TYPE_REST, rating = 4.6),
        Place(placeId = "rest_3", name = "Parc des Buttes-Chaumont", address = "1 Rue Botzaris, 75019 Paris", latitude = 48.8809, longitude = 2.3828, stopType = Place.STOP_TYPE_REST, rating = 4.7),
        Place(placeId = "rest_4", name = "Champ de Mars Garden", address = "2 Allée Adrienne Lecouvreur, 75007 Paris", latitude = 48.8556, longitude = 2.2986, stopType = Place.STOP_TYPE_REST, rating = 4.5)
    )

    suspend fun searchPlaces(context: Context, query: String): List<Place> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext SAMPLE_SIGHTSEEING

        val results = mutableListOf<Place>()
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocationName(query, 5)

            if (!addresses.isNullOrEmpty()) {
                addresses.forEachIndexed { index, addr ->
                    val name = addr.featureName ?: addr.locality ?: query
                    val fullAddress = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
                    results.add(
                        Place(
                            placeId = "geo_${System.currentTimeMillis()}_$index",
                            name = name,
                            address = if (fullAddress.isNotBlank()) fullAddress else "$name (${addr.countryName ?: ""})",
                            latitude = addr.latitude,
                            longitude = addr.longitude,
                            stopType = Place.STOP_TYPE_SIGHTSEEING,
                            rating = 4.5
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Geocoder exception or offline
        }

        if (results.isEmpty()) {
            // Filter sample places matching query
            val matched = SAMPLE_SIGHTSEEING.filter {
                it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
            }
            if (matched.isNotEmpty()) return@withContext matched

            // Custom place created from query if not found
            return@withContext listOf(
                Place(
                    placeId = "custom_${System.currentTimeMillis()}",
                    name = query,
                    address = "Search result for '$query'",
                    latitude = 48.8566 + (Math.random() - 0.5) * 0.05,
                    longitude = 2.3522 + (Math.random() - 0.5) * 0.05,
                    stopType = Place.STOP_TYPE_SIGHTSEEING,
                    rating = 4.0
                )
            )
        }
        return@withContext results
    }

    suspend fun searchNearbyFood(context: Context, lat: Double, lng: Double): List<Place> = withContext(Dispatchers.IO) {
        // Find food places ordered by proximity to (lat, lng)
        val sorted = SAMPLE_FOOD_STOPS.map { place ->
            val dist = RouteOptimizer.calculateDistanceKm(lat, lng, place.latitude, place.longitude)
            val roundedDist = (dist * 10).roundToInt() / 10.0
            place.copy(address = "${place.address} ($roundedDist km away)")
        }.sortedBy { RouteOptimizer.calculateDistanceKm(lat, lng, it.latitude, it.longitude) }

        return@withContext sorted
    }

    suspend fun searchNearbyRest(context: Context, lat: Double, lng: Double): List<Place> = withContext(Dispatchers.IO) {
        val sorted = SAMPLE_REST_STOPS.map { place ->
            val dist = RouteOptimizer.calculateDistanceKm(lat, lng, place.latitude, place.longitude)
            val roundedDist = (dist * 10).roundToInt() / 10.0
            place.copy(address = "${place.address} ($roundedDist km away)")
        }.sortedBy { RouteOptimizer.calculateDistanceKm(lat, lng, it.latitude, it.longitude) }

        return@withContext sorted
    }
}
