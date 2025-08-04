package com.proxilocal.hyperlocal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Manages location fetching using Google's Fused Location Provider.
 *
 * @param context The application context, used to access location services.
 */
class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val TAG = "LocationHelper"

    /**
     * Fetches the current device location with high accuracy.
     * Requires [Manifest.permission.ACCESS_FINE_LOCATION].
     *
     * @param onLocationFetched Callback invoked with the fetched [Location] if successful.
     * @param onLocationError Callback invoked with an error message if location fetching fails
     *                       (e.g., missing permissions or no location available).
     */
    fun fetchCurrentLocation(
        onLocationFetched: (Location) -> Unit,
        onLocationError: (String) -> Unit = {}
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission")
            onLocationError("Location permission not granted")
            return
        }

        val cancellationTokenSource = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(TAG, "Location fetched: ${location.latitude}, ${location.longitude}")
                onLocationFetched(location)
            } else {
                Log.w(TAG, "No location available")
                onLocationError("No location available")
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to fetch location", exception)
            onLocationError("Failed to fetch location: ${exception.message}")
        }.addOnCanceledListener {
            Log.w(TAG, "Location request canceled")
            onLocationError("Location request canceled")
        }
    }
}