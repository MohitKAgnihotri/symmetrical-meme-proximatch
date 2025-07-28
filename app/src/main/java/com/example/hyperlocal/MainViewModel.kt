package com.example.hyperlocal

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import com.example.ui.components.RadarTheme
import com.example.ui.components.ThemeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private lateinit var bleAdvertiser: BLEAdvertiser
    private lateinit var bleScanner: BLEScanner
    private lateinit var locationHelper: LocationHelper

    private val _matchResults = MutableStateFlow<List<MatchResult>>(emptyList())
    val matchResults: StateFlow<List<MatchResult>> = _matchResults

    private val _selectedTheme = MutableStateFlow(ThemeProvider.NeonTech)
    val selectedTheme: StateFlow<RadarTheme> = _selectedTheme

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation

    // --- NEW: State to control the radar sweep animation ---
    private val _isSweeping = MutableStateFlow(false)
    val isSweeping: StateFlow<Boolean> = _isSweeping

    // --- NEW: State to store the last location that triggered a zoom ---
    private var lastZoomLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (!::bleAdvertiser.isInitialized) bleAdvertiser = BLEAdvertiser(context)
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(context) { match ->
                // Avoid adding duplicate matches
                val currentMatches = _matchResults.value
                if (currentMatches.none { it.id == match.id }) {
                    _matchResults.value = currentMatches + match
                }
            }
        }

        // Start scanning and begin the sweep animation
        bleScanner.startScanning()
        _isSweeping.value = true

        // Fetch location to check for zoom animation
        fetchUserLocation(context)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (::bleScanner.isInitialized) bleScanner.stopScanning()
        // Stop the sweep, but keep the dots on screen
        _isSweeping.value = false
    }

    @SuppressLint("MissingPermission")
    fun fetchUserLocation(context: Context) {
        if (!::locationHelper.isInitialized) locationHelper = LocationHelper(context)

        locationHelper.fetchCurrentLocation { newLocation ->
            val lastLocation = lastZoomLocation
            // Trigger zoom if it's the first time, or if distance is > 5km
            if (lastLocation == null || lastLocation.distanceTo(newLocation) > 5000) {
                _userLocation.value = newLocation
                lastZoomLocation = newLocation
            }
        }
    }
}