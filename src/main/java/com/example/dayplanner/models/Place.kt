package com.example.dayplanner.models

data class Place(
    val id: Long = 0,
    val placeId: String = "",
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    var isCompleted: Boolean = false,
    val stopType: String = STOP_TYPE_SIGHTSEEING, // SIGHTSEEING, FOOD, REST
    val rating: Double = 0.0,
    var visitOrder: Int = -1
) {
    companion object {
        const val STOP_TYPE_SIGHTSEEING = "SIGHTSEEING"
        const val STOP_TYPE_FOOD = "FOOD"
        const val STOP_TYPE_REST = "REST"
    }

    fun isSightseeing(): Boolean = stopType == STOP_TYPE_SIGHTSEEING
    fun isFoodStop(): Boolean = stopType == STOP_TYPE_FOOD
    fun isRestStop(): Boolean = stopType == STOP_TYPE_REST
}
