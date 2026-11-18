package com.example.dayplanner.utils

import com.example.dayplanner.models.Place
import com.example.dayplanner.models.RouteResult
import kotlin.math.*

object RouteOptimizer {

    /**
     * Optimizes visiting order using Nearest-Neighbor + 2-Opt TSP heuristic.
     * Starts from the user's current location (if available) or the first place in the list.
     */
    fun optimizeRoute(
        places: List<Place>,
        startLat: Double? = null,
        startLng: Double? = null
    ): RouteResult {
        if (places.isEmpty()) {
            return RouteResult(emptyList(), emptyList(), 0.0, 0, "No places in itinerary")
        }

        if (places.size == 1) {
            val place = places.first()
            place.visitOrder = 1
            val dist = if (startLat != null && startLng != null) {
                calculateDistanceKm(startLat, startLng, place.latitude, place.longitude)
            } else 0.0
            val duration = (dist / 40.0 * 60).roundToInt() + 30
            val poly = if (startLat != null && startLng != null) {
                listOf(Pair(startLat, startLng), Pair(place.latitude, place.longitude))
            } else {
                listOf(Pair(place.latitude, place.longitude))
            }
            return RouteResult(
                orderedPlaces = listOf(place),
                polylinePoints = poly,
                totalDistanceKm = (dist * 10).roundToInt() / 10.0,
                totalDurationMinutes = duration,
                summary = "1 stop • ${(dist * 10).roundToInt() / 10.0} km • ~$duration mins"
            )
        }

        val unvisited = places.toMutableList()
        val ordered = mutableListOf<Place>()

        // 1. Determine start point
        var currentLat = startLat ?: places.first().latitude
        var currentLng = startLng ?: places.first().longitude

        // If no explicit start location, pick first place as start
        if (startLat == null || startLng == null) {
            val first = unvisited.removeAt(0)
            ordered.add(first)
            currentLat = first.latitude
            currentLng = first.longitude
        }

        // 2. Greedy Nearest Neighbor
        while (unvisited.isNotEmpty()) {
            var nearestIndex = -1
            var minDistance = Double.MAX_VALUE

            for (i in unvisited.indices) {
                val p = unvisited[i]
                val dist = calculateDistanceKm(currentLat, currentLng, p.latitude, p.longitude)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestIndex = i
                }
            }

            if (nearestIndex != -1) {
                val nextPlace = unvisited.removeAt(nearestIndex)
                ordered.add(nextPlace)
                currentLat = nextPlace.latitude
                currentLng = nextPlace.longitude
            }
        }

        // 3. Apply 2-Opt heuristic to refine order
        val refinedOrder = twoOptOptimize(ordered)

        // Assign 1-based visit order
        refinedOrder.forEachIndexed { index, place ->
            place.visitOrder = index + 1
        }

        // 4. Calculate total distance & duration
        var totalDistKm = 0.0
        val polyline = mutableListOf<Pair<Double, Double>>()

        if (startLat != null && startLng != null) {
            polyline.add(Pair(startLat, startLng))
            totalDistKm += calculateDistanceKm(startLat, startLng, refinedOrder.first().latitude, refinedOrder.first().longitude)
        }

        for (i in refinedOrder.indices) {
            val p = refinedOrder[i]
            polyline.add(Pair(p.latitude, p.longitude))
            if (i > 0) {
                val prev = refinedOrder[i - 1]
                totalDistKm += calculateDistanceKm(prev.latitude, prev.longitude, p.latitude, p.longitude)
            }
        }

        // Calculate travel duration (assuming 35 km/h city speed + 30 mins per stop)
        val drivingTimeMins = (totalDistKm / 35.0 * 60).roundToInt()
        val stopOverheadMins = refinedOrder.size * 30
        val totalDurationMins = drivingTimeMins + stopOverheadMins

        val roundedDist = (totalDistKm * 10).roundToInt() / 10.0
        val summaryStr = "${refinedOrder.size} stops • $roundedDist km • ~${totalDurationMins / 60}h ${totalDurationMins % 60}m"

        return RouteResult(
            orderedPlaces = refinedOrder,
            polylinePoints = generateDetailedPolyline(polyline),
            totalDistanceKm = roundedDist,
            totalDurationMinutes = totalDurationMins,
            summary = summaryStr
        )
    }

    private fun twoOptOptimize(route: List<Place>): List<Place> {
        if (route.size <= 3) return route
        val current = route.toMutableList()
        var improved = true
        var iterations = 0

        while (improved && iterations < 50) {
            improved = false
            iterations++
            for (i in 0 until current.size - 1) {
                for (j in i + 2 until current.size) {
                    val d1 = calculateDistanceKm(current[i].latitude, current[i].longitude, current[i + 1].latitude, current[i + 1].longitude) +
                             calculateDistanceKm(current[j].latitude, current[j].longitude, current[(j + 1) % current.size].latitude, current[(j + 1) % current.size].longitude)
                    val d2 = calculateDistanceKm(current[i].latitude, current[i].longitude, current[j].latitude, current[j].longitude) +
                             calculateDistanceKm(current[i + 1].latitude, current[i + 1].longitude, current[(j + 1) % current.size].latitude, current[(j + 1) % current.size].longitude)
                    if (d2 < d1) {
                        current.subList(i + 1, j + 1).reverse()
                        improved = true
                    }
                }
            }
        }
        return current
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Interpolates smooth waypoints along paths between points.
     */
    private fun generateDetailedPolyline(waypoints: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        if (waypoints.size < 2) return waypoints
        val result = mutableListOf<Pair<Double, Double>>()

        for (i in 0 until waypoints.size - 1) {
            val p1 = waypoints[i]
            val p2 = waypoints[i + 1]
            result.add(p1)

            // Add intermediate curved point to simulate real road segment curve
            val midLat = (p1.first + p2.first) / 2.0 + (p2.second - p1.second) * 0.05
            val midLng = (p1.second + p2.second) / 2.0 - (p2.first - p1.first) * 0.05
            result.add(Pair(midLat, midLng))
        }
        result.add(waypoints.last())
        return result
    }
}
