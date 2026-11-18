package com.example.dayplanner.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dayplanner.models.Place
import com.example.dayplanner.models.RouteResult
import com.example.dayplanner.repositories.PlaceRepository
import com.example.dayplanner.utils.DirectionsHelper
import com.example.dayplanner.utils.LocationHelper
import com.example.dayplanner.utils.PlacesSearchHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlaceRepository(application)
    private val locationHelper = LocationHelper(application)

    private val _placesState = MutableStateFlow<List<Place>>(emptyList())
    val placesState: StateFlow<List<Place>> = _placesState.asStateFlow()

    private val _routeResultState = MutableStateFlow<RouteResult?>(null)
    val routeResultState: StateFlow<RouteResult?> = _routeResultState.asStateFlow()

    private val _userLocationState = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocationState: StateFlow<Pair<Double, Double>?> = _userLocationState.asStateFlow()

    private val _searchResultsState = MutableStateFlow<List<Place>>(emptyList())
    val searchResultsState: StateFlow<List<Place>> = _searchResultsState.asStateFlow()

    private val _nearbyFoodState = MutableStateFlow<List<Place>>(emptyList())
    val nearbyFoodState: StateFlow<List<Place>> = _nearbyFoodState.asStateFlow()

    private val _nearbyRestState = MutableStateFlow<List<Place>>(emptyList())
    val nearbyRestState: StateFlow<List<Place>> = _nearbyRestState.asStateFlow()

    private val _isLoadingState = MutableStateFlow(false)
    val isLoadingState: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    private val _userMessageState = MutableStateFlow<String?>(null)
    val userMessageState: StateFlow<String?> = _userMessageState.asStateFlow()

    private val _selectedTabState = MutableStateFlow(0)
    val selectedTabState: StateFlow<Int> = _selectedTabState.asStateFlow()

    init {
        loadPlaces()
        loadNearbyDefaults()
        fetchLocation()
    }

    fun setSelectedTab(tabPosition: Int) {
        _selectedTabState.value = tabPosition
    }

    fun clearUserMessage() {
        _userMessageState.value = null
    }

    fun fetchLocation() {
        viewModelScope.launch {
            if (locationHelper.hasLocationPermission()) {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    _userLocationState.value = Pair(loc.latitude, loc.longitude)
                } else {
                    // Default fallback location (Paris center) if GPS is unavailable
                    _userLocationState.value = Pair(48.8566, 2.3522)
                }
            } else {
                _userLocationState.value = Pair(48.8566, 2.3522)
            }
        }
    }

    fun loadPlaces() {
        viewModelScope.launch {
            _isLoadingState.value = true
            val list = repository.getAllPlaces()
            _placesState.value = list

            // Check if saved itinerary exists
            val savedItinerary = repository.getSavedItinerary()
            if (savedItinerary.isNotEmpty()) {
                val route = DirectionsHelper.getOptimizedRoute(
                    savedItinerary,
                    _userLocationState.value?.first,
                    _userLocationState.value?.second
                )
                _routeResultState.value = route
            }
            _isLoadingState.value = false
        }
    }

    fun searchPlaces(query: String) {
        viewModelScope.launch {
            _isLoadingState.value = true
            val results = PlacesSearchHelper.searchPlaces(getApplication(), query)
            _searchResultsState.value = results
            _isLoadingState.value = false
        }
    }

    fun addPlace(place: Place) {
        viewModelScope.launch {
            // Avoid adding duplicates by name or coordinates
            val existing = _placesState.value.find {
                it.name.equals(place.name, ignoreCase = true) ||
                (Math.abs(it.latitude - place.latitude) < 0.0001 && Math.abs(it.longitude - place.longitude) < 0.0001)
            }
            if (existing != null) {
                _userMessageState.value = "'${place.name}' is already in your itinerary!"
                return@launch
            }

            val savedPlace = repository.addPlace(place)
            val updatedList = _placesState.value + savedPlace
            _placesState.value = updatedList
            _userMessageState.value = "Added '${place.name}' to your itinerary"

            // Recalculate route if route was already active
            if (_routeResultState.value != null) {
                planMyDay()
            }
        }
    }

    fun togglePlaceCompleted(place: Place, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updatePlaceCompleted(place.id, isCompleted)
            val updatedList = _placesState.value.map {
                if (it.id == place.id) it.copy(isCompleted = isCompleted) else it
            }
            _placesState.value = updatedList

            // Also update in route result if present
            _routeResultState.value?.let { currentRoute ->
                val updatedOrdered = currentRoute.orderedPlaces.map {
                    if (it.id == place.id) it.copy(isCompleted = isCompleted) else it
                }
                _routeResultState.value = currentRoute.copy(orderedPlaces = updatedOrdered)
            }
        }
    }

    fun removePlace(placeId: Long) {
        viewModelScope.launch {
            val placeToRemove = _placesState.value.find { it.id == placeId }
            repository.deletePlace(placeId)
            val updatedList = _placesState.value.filter { it.id != placeId }
            _placesState.value = updatedList

            _userMessageState.value = placeToRemove?.let { "Removed '${it.name}'" } ?: "Place removed"

            if (updatedList.isEmpty()) {
                clearRoute()
            } else if (_routeResultState.value != null) {
                planMyDay()
            }
        }
    }

    fun planMyDay() {
        viewModelScope.launch {
            val places = _placesState.value
            if (places.isEmpty()) {
                _userMessageState.value = "Please add places to your itinerary first!"
                return@launch
            }

            _isLoadingState.value = true
            val (startLat, startLng) = _userLocationState.value ?: Pair(places.first().latitude, places.first().longitude)

            val route = DirectionsHelper.getOptimizedRoute(places, startLat, startLng)
            _routeResultState.value = route

            // Save ordered itinerary to SQLite
            repository.saveItinerary(route.orderedPlaces)

            _isLoadingState.value = false
            _userMessageState.value = "Route planned! ${route.orderedPlaces.size} stops total."
        }
    }

    fun searchNearbyFood() {
        viewModelScope.launch {
            val (lat, lng) = _userLocationState.value ?: Pair(48.8566, 2.3522)
            val results = PlacesSearchHelper.searchNearbyFood(getApplication(), lat, lng)
            _nearbyFoodState.value = results
        }
    }

    fun searchNearbyRest() {
        viewModelScope.launch {
            val (lat, lng) = _userLocationState.value ?: Pair(48.8566, 2.3522)
            val results = PlacesSearchHelper.searchNearbyRest(getApplication(), lat, lng)
            _nearbyRestState.value = results
        }
    }

    private fun loadNearbyDefaults() {
        searchNearbyFood()
        searchNearbyRest()
    }

    fun clearRoute() {
        viewModelScope.launch {
            repository.clearItinerary()
            _routeResultState.value = null
            // Reset visit orders
            val resetList = _placesState.value.map { it.copy(visitOrder = -1) }
            _placesState.value = resetList
            _userMessageState.value = "Route cleared."
        }
    }

    fun loadSamplePlaces() {
        viewModelScope.launch {
            _isLoadingState.value = true
            repository.clearAllPlaces()
            PlacesSearchHelper.SAMPLE_SIGHTSEEING.take(4).forEach {
                repository.addPlace(it)
            }
            loadPlaces()
            _userMessageState.value = "Loaded sample Paris sightseeing itinerary!"
        }
    }
}
