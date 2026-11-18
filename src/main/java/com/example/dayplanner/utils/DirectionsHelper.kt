package com.example.dayplanner.utils

import com.example.dayplanner.BuildConfig
import com.example.dayplanner.models.Place
import com.example.dayplanner.models.RouteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object DirectionsHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getOptimizedRoute(
        places: List<Place>,
        startLat: Double? = null,
        startLng: Double? = null
    ): RouteResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE" || places.size <= 1) {
            return@withContext RouteOptimizer.optimizeRoute(places, startLat, startLng)
        }

        try {
            val origin = if (startLat != null && startLng != null) {
                "$startLat,$startLng"
            } else {
                "${places.first().latitude},${places.first().longitude}"
            }

            val destination = "${places.last().latitude},${places.last().longitude}"

            val waypointsList = if (startLat != null && startLng != null) {
                places
            } else {
                places.drop(1).dropLast(1)
            }

            val waypointsStr = if (waypointsList.isNotEmpty()) {
                "optimize:true|" + waypointsList.joinToString("|") { "${it.latitude},${it.longitude}" }
            } else ""

            var url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$destination&key=$apiKey"
            if (waypointsStr.isNotEmpty()) {
                url += "&waypoints=$waypointsStr"
            }

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val jsonStr = response.body?.string()

            if (!response.isSuccessful || jsonStr.isNullOrEmpty()) {
                return@withContext RouteOptimizer.optimizeRoute(places, startLat, startLng)
            }

            val json = JSONObject(jsonStr)
            val status = json.optString("status")
            if (status != "OK") {
                return@withContext RouteOptimizer.optimizeRoute(places, startLat, startLng)
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return@withContext RouteOptimizer.optimizeRoute(places, startLat, startLng)
            }

            val route0 = routes.getJSONObject(0)
            val waypointOrder = route0.optJSONArray("waypoint_order")

            val legs = route0.getJSONArray("legs")
            var totalMeters = 0.0
            var totalSeconds = 0

            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                totalMeters += leg.getJSONObject("distance").getDouble("value")
                totalSeconds += leg.getJSONObject("duration").getInt("value")
            }

            val overviewPolyline = route0.getJSONObject("overview_polyline").getString("points")
            val decodedPoly = decodePolyline(overviewPolyline)

            // Reorder places based on API's waypoint_order
            val orderedPlaces = mutableListOf<Place>()
            if (waypointOrder != null && waypointsList.isNotEmpty()) {
                if (startLat == null || startLng == null) {
                    orderedPlaces.add(places.first())
                }
                for (i in 0 until waypointOrder.length()) {
                    val origIndex = waypointOrder.getInt(i)
                    if (origIndex < waypointsList.size) {
                        orderedPlaces.add(waypointsList[origIndex])
                    }
                }
                if (startLat == null || startLng == null) {
                    orderedPlaces.add(places.last())
                }
            } else {
                orderedPlaces.addAll(places)
            }

            orderedPlaces.forEachIndexed { index, place ->
                place.visitOrder = index + 1
            }

            val totalKm = (totalMeters / 100.0).roundToInt() / 10.0
            val totalMins = (totalSeconds / 60.0).roundToInt()

            RouteResult(
                orderedPlaces = orderedPlaces,
                polylinePoints = decodedPoly,
                totalDistanceKm = totalKm,
                totalDurationMinutes = totalMins,
                summary = "${orderedPlaces.size} stops • $totalKm km • ~${totalMins / 60}h ${totalMins % 60}m"
            )
        } catch (e: Exception) {
            RouteOptimizer.optimizeRoute(places, startLat, startLng)
        }
    }

    /**
     * Standard Polyline algorithm decoder.
     */
    private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val poly = mutableListOf<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val pLat = lat.toDouble() / 1E5
            val pLng = lng.toDouble() / 1E5
            poly.add(Pair(pLat, pLng))
        }
        return poly
    }
}
