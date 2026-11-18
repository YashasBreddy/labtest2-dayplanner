package com.example.dayplanner.repositories

import android.content.Context
import com.example.dayplanner.database.DayPlannerDatabaseHelper
import com.example.dayplanner.models.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaceRepository(context: Context) {

    private val dbHelper = DayPlannerDatabaseHelper(context.applicationContext)

    suspend fun getAllPlaces(): List<Place> = withContext(Dispatchers.IO) {
        dbHelper.getAllPlaces()
    }

    suspend fun addPlace(place: Place): Place = withContext(Dispatchers.IO) {
        val insertedId = dbHelper.insertPlace(place)
        place.copy(id = insertedId)
    }

    suspend fun updatePlaceCompleted(id: Long, isCompleted: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.updatePlaceCompleted(id, isCompleted) > 0
        }

    suspend fun deletePlace(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deletePlace(id) > 0
    }

    suspend fun saveItinerary(orderedPlaces: List<Place>) = withContext(Dispatchers.IO) {
        dbHelper.saveItinerary(orderedPlaces)
    }

    suspend fun getSavedItinerary(): List<Place> = withContext(Dispatchers.IO) {
        dbHelper.getSavedItinerary()
    }

    suspend fun clearItinerary() = withContext(Dispatchers.IO) {
        dbHelper.clearItinerary()
    }

    suspend fun clearAllPlaces() = withContext(Dispatchers.IO) {
        dbHelper.clearAllPlaces()
    }
}
