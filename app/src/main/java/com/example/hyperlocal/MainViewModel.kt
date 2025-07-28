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

    // StateFlow to hold the user's current location
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (!::bleAdvertiser.isInitialized) {
            bleAdvertiser = BLEAdvertiser(context)
        }
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(context) { match ->
                val updated = _matchResults.value.toMutableList().apply { add(match) }
                _matchResults.value = updated
            }
        }

        val criteria = CriteriaManager.getEncodedCriteria(context)
        val senderId = UserIdManager.getOrGenerateId(context)
        bleAdvertiser.startAdvertising(criteria, senderId)
        bleScanner.startScanning()
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (::bleAdvertiser.isInitialized) {
            bleAdvertiser.stopAdvertising()
        }
        if (::bleScanner.isInitialized) {
            bleScanner.stopScanning()
        }
        _matchResults.value = emptyList()
    }

    // Function to initialize and use the LocationHelper to get coordinates
    @SuppressLint("MissingPermission")
    fun fetchUserLocation(context: Context) {
        if (!::locationHelper.isInitialized) {
            locationHelper = LocationHelper(context)
        }
        locationHelper.fetchCurrentLocation { location ->
            _userLocation.value = location
        }
    }
}