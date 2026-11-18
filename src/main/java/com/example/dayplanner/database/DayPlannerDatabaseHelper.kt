package com.example.dayplanner.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.dayplanner.models.Place

class DayPlannerDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "dayplanner.db"
        const val DATABASE_VERSION = 2

        // Table Places
        const val TABLE_PLACES = "places"
        const val COLUMN_ID = "id"
        const val COLUMN_PLACE_ID = "place_id"
        const val COLUMN_PLACE_NAME = "place_name"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_STOP_TYPE = "stop_type"
        const val COLUMN_RATING = "rating"

        // Table Itinerary
        const val TABLE_ITINERARY = "itinerary"
        const val COLUMN_ITINERARY_ID = "id"
        const val COLUMN_ITINERARY_PLACE_ID = "place_id"
        const val COLUMN_VISIT_ORDER = "visit_order"
        const val COLUMN_ITINERARY_STOP_TYPE = "stop_type"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createPlacesTable = """
            CREATE TABLE $TABLE_PLACES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PLACE_ID TEXT,
                $COLUMN_PLACE_NAME TEXT NOT NULL,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_LATITUDE REAL NOT NULL,
                $COLUMN_LONGITUDE REAL NOT NULL,
                $COLUMN_IS_COMPLETED INTEGER DEFAULT 0,
                $COLUMN_STOP_TYPE TEXT DEFAULT 'SIGHTSEEING',
                $COLUMN_RATING REAL DEFAULT 0.0
            )
        """.trimIndent()

        val createItineraryTable = """
            CREATE TABLE $TABLE_ITINERARY (
                $COLUMN_ITINERARY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ITINERARY_PLACE_ID INTEGER NOT NULL,
                $COLUMN_VISIT_ORDER INTEGER NOT NULL,
                $COLUMN_ITINERARY_STOP_TYPE TEXT,
                FOREIGN KEY($COLUMN_ITINERARY_PLACE_ID) REFERENCES $TABLE_PLACES($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createPlacesTable)
        db.execSQL(createItineraryTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITINERARY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLACES")
        onCreate(db)
    }

    fun insertPlace(place: Place): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PLACE_ID, place.placeId)
            put(COLUMN_PLACE_NAME, place.name)
            put(COLUMN_ADDRESS, place.address)
            put(COLUMN_LATITUDE, place.latitude)
            put(COLUMN_LONGITUDE, place.longitude)
            put(COLUMN_IS_COMPLETED, if (place.isCompleted) 1 else 0)
            put(COLUMN_STOP_TYPE, place.stopType)
            put(COLUMN_RATING, place.rating)
        }
        return db.insert(TABLE_PLACES, null, values)
    }

    fun getAllPlaces(): List<Place> {
        val list = mutableListOf<Place>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PLACES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ID ASC"
        )

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
            val placeIdIndex = c.getColumnIndexOrThrow(COLUMN_PLACE_ID)
            val nameIndex = c.getColumnIndexOrThrow(COLUMN_PLACE_NAME)
            val addressIndex = c.getColumnIndexOrThrow(COLUMN_ADDRESS)
            val latIndex = c.getColumnIndexOrThrow(COLUMN_LATITUDE)
            val lngIndex = c.getColumnIndexOrThrow(COLUMN_LONGITUDE)
            val completedIndex = c.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)
            val stopTypeIndex = c.getColumnIndexOrThrow(COLUMN_STOP_TYPE)
            val ratingIndex = c.getColumnIndexOrThrow(COLUMN_RATING)

            while (c.moveToNext()) {
                val place = Place(
                    id = c.getLong(idIndex),
                    placeId = c.getString(placeIdIndex) ?: "",
                    name = c.getString(nameIndex) ?: "",
                    address = c.getString(addressIndex) ?: "",
                    latitude = c.getDouble(latIndex),
                    longitude = c.getDouble(lngIndex),
                    isCompleted = c.getInt(completedIndex) == 1,
                    stopType = c.getString(stopTypeIndex) ?: Place.STOP_TYPE_SIGHTSEEING,
                    rating = c.getDouble(ratingIndex)
                )
                list.add(place)
            }
        }
        return list
    }

    fun updatePlaceCompleted(id: Long, isCompleted: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_COMPLETED, if (isCompleted) 1 else 0)
        }
        return db.update(TABLE_PLACES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deletePlace(id: Long): Int {
        val db = writableDatabase
        db.delete(TABLE_ITINERARY, "$COLUMN_ITINERARY_PLACE_ID = ?", arrayOf(id.toString()))
        return db.delete(TABLE_PLACES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun saveItinerary(orderedPlaces: List<Place>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_ITINERARY, null, null)
            orderedPlaces.forEachIndexed { index, place ->
                val values = ContentValues().apply {
                    put(COLUMN_ITINERARY_PLACE_ID, place.id)
                    put(COLUMN_VISIT_ORDER, index + 1)
                    put(COLUMN_ITINERARY_STOP_TYPE, place.stopType)
                }
                db.insert(TABLE_ITINERARY, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getSavedItinerary(): List<Place> {
        val result = mutableListOf<Place>()
        val allPlacesMap = getAllPlaces().associateBy { it.id }
        val db = readableDatabase

        val cursor = db.query(
            TABLE_ITINERARY,
            arrayOf(COLUMN_ITINERARY_PLACE_ID, COLUMN_VISIT_ORDER),
            null,
            null,
            null,
            null,
            "$COLUMN_VISIT_ORDER ASC"
        )

        cursor.use { c ->
            val placeIdIndex = c.getColumnIndexOrThrow(COLUMN_ITINERARY_PLACE_ID)
            val orderIndex = c.getColumnIndexOrThrow(COLUMN_VISIT_ORDER)

            while (c.moveToNext()) {
                val placeId = c.getLong(placeIdIndex)
                val order = c.getInt(orderIndex)
                allPlacesMap[placeId]?.let { place ->
                    place.visitOrder = order
                    result.add(place)
                }
            }
        }
        return result
    }

    fun clearItinerary() {
        val db = writableDatabase
        db.delete(TABLE_ITINERARY, null, null)
    }

    fun clearAllPlaces() {
        val db = writableDatabase
        db.delete(TABLE_ITINERARY, null, null)
        db.delete(TABLE_PLACES, null, null)
    }
}
