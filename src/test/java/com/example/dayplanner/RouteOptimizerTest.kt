package com.example.dayplanner

import com.example.dayplanner.models.Place
import com.example.dayplanner.utils.RouteOptimizer
import org.junit.Assert.*
import org.junit.Test

class RouteOptimizerTest {

    @Test
    fun testEmptyPlacesReturnsEmptyRoute() {
        val result = RouteOptimizer.optimizeRoute(emptyList())
        assertTrue(result.orderedPlaces.isEmpty())
        assertEquals(0.0, result.totalDistanceKm, 0.01)
    }

    @Test
    fun testSinglePlaceRoute() {
        val place = Place(
            id = 1,
            name = "Eiffel Tower",
            address = "Paris",
            latitude = 48.8584,
            longitude = 2.2945
        )

        val result = RouteOptimizer.optimizeRoute(listOf(place), startLat = 48.8566, startLng = 2.3522)
        assertEquals(1, result.orderedPlaces.size)
        assertEquals(1, result.orderedPlaces.first().visitOrder)
        assertTrue(result.totalDistanceKm > 0.0)
    }

    @Test
    fun testRouteOptimizationOrderingWithFoodAndRestStops() {
        val sightseeing1 = Place(id = 1, name = "Eiffel Tower", address = "Paris", latitude = 48.8584, longitude = 2.2945)
        val sightseeing2 = Place(id = 2, name = "Louvre Museum", address = "Paris", latitude = 48.8606, longitude = 2.3376)
        val foodStop = Place(id = 3, name = "Bistro Parisien", address = "Paris", latitude = 48.8596, longitude = 2.2936, stopType = Place.STOP_TYPE_FOOD)
        val restStop = Place(id = 4, name = "Luxembourg Garden", address = "Paris", latitude = 48.8462, longitude = 2.3372, stopType = Place.STOP_TYPE_REST)

        val places = listOf(sightseeing1, sightseeing2, foodStop, restStop)
        val result = RouteOptimizer.optimizeRoute(places, startLat = 48.8566, startLng = 2.3522)

        assertEquals(4, result.orderedPlaces.size)
        assertTrue(result.polylinePoints.isNotEmpty())
        assertTrue(result.totalDistanceKm > 0.0)
        assertTrue(result.totalDurationMinutes > 0)

        // Verify each place has a 1-based visit order
        result.orderedPlaces.forEachIndexed { index, place ->
            assertEquals(index + 1, place.visitOrder)
        }
    }

    @Test
    fun testHaversineDistanceCalculation() {
        // Distance between Eiffel Tower and Louvre Museum is approx 3.4 km
        val distance = RouteOptimizer.calculateDistanceKm(48.8584, 2.2945, 48.8606, 2.3376)
        assertEquals(3.4, distance, 0.5)
    }
}
