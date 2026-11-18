package com.example.dayplanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.example.dayplanner.R
import com.example.dayplanner.models.Place
import com.example.dayplanner.models.RouteResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

object MapsHelper {

    fun renderPlacesOnMap(
        context: Context,
        map: GoogleMap,
        places: List<Place>,
        routeResult: RouteResult? = null,
        userLocation: Pair<Double, Double>? = null
    ) {
        map.clear()
        if (places.isEmpty() && userLocation == null) return

        val builder = LatLngBounds.Builder()
        var hasBounds = false

        // 1. Plot user location if available
        userLocation?.let { (lat, lng) ->
            val pos = LatLng(lat, lng)
            map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Your Location")
                    .snippet("Current starting location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            builder.include(pos)
            hasBounds = true
        }

        // 2. Draw polyline if route result exists
        routeResult?.let { route ->
            if (route.polylinePoints.isNotEmpty()) {
                val polyOptions = PolylineOptions()
                    .width(12f)
                    .color(ContextCompat.getColor(context, R.color.primary_blue))
                    .jointType(JointType.ROUND)

                route.polylinePoints.forEach { (lat, lng) ->
                    val pos = LatLng(lat, lng)
                    polyOptions.add(pos)
                    builder.include(pos)
                    hasBounds = true
                }
                map.addPolyline(polyOptions)
            }
        }

        // 3. Add place markers
        places.forEach { place ->
            val pos = LatLng(place.latitude, place.longitude)
            val markerOptions = MarkerOptions().position(pos)

            val titlePrefix = if (place.visitOrder > 0) "Stop #${place.visitOrder}: " else ""
            markerOptions.title("$titlePrefix${place.name}")

            val statusText = when {
                place.isCompleted -> "Status: Completed ✓"
                place.isFoodStop() -> "Food Stop • Rating: ${place.rating}"
                place.isRestStop() -> "Rest Stop • Rating: ${place.rating}"
                else -> "Sightseeing • ${place.address}"
            }
            markerOptions.snippet(statusText)

            // Bitmap icon selection
            val iconBitmap = if (place.visitOrder > 0) {
                createNumberedMarkerBitmap(context, place.visitOrder, getMarkerColor(context, place))
            } else {
                createColoredMarkerBitmap(context, getMarkerColor(context, place), place)
            }

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconBitmap))
            map.addMarker(markerOptions)

            builder.include(pos)
            hasBounds = true
        }

        // 4. Animate camera to fit all bounds
        if (hasBounds) {
            try {
                val bounds = builder.build()
                val padding = 120
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (e: Exception) {
                // If bounds math fails due to single point
                val targetPos = if (places.isNotEmpty()) {
                    LatLng(places.first().latitude, places.first().longitude)
                } else if (userLocation != null) {
                    LatLng(userLocation.first, userLocation.second)
                } else {
                    LatLng(48.8566, 2.3522)
                }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(targetPos, 13f))
            }
        }
    }

    private fun getMarkerColor(context: Context, place: Place): Int {
        return when {
            place.isCompleted -> ContextCompat.getColor(context, R.color.stop_completed)
            place.isFoodStop() -> ContextCompat.getColor(context, R.color.stop_food)
            place.isRestStop() -> ContextCompat.getColor(context, R.color.stop_rest)
            else -> ContextCompat.getColor(context, R.color.stop_sightseeing)
        }
    }

    private fun createColoredMarkerBitmap(context: Context, color: Int, place: Place): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        // Draw pin circle
        val radius = size / 2f - 8f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        canvas.drawCircle(size / 2f, size / 2f, radius, borderPaint)

        // Draw inner indicator or checkmark if completed
        if (place.isCompleted) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 42f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✓", size / 2f, size / 2f + 14f, textPaint)
        } else {
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size / 2f, size / 2f, radius / 3f, innerPaint)
        }

        return bitmap
    }

    private fun createNumberedMarkerBitmap(context: Context, number: Int, color: Int): Bitmap {
        val size = 110
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        val radius = size / 2f - 10f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)
        canvas.drawCircle(size / 2f, size / 2f, radius, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val bounds = Rect()
        val textStr = number.toString()
        textPaint.getTextBounds(textStr, 0, textStr.length, bounds)
        val y = size / 2f + bounds.height() / 2f - 4f

        canvas.drawText(textStr, size / 2f, y, textPaint)

        return bitmap
    }
}
