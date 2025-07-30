package com.example.hyperlocal

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
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

    private val _isSweeping = MutableStateFlow(false)
    val isSweeping: StateFlow<Boolean> = _isSweeping

    private var lastZoomLocation: Location? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun start(context: Context) {
        if (!::bleAdvertiser.isInitialized) bleAdvertiser = BLEAdvertiser(context)
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(context) { match ->
                val currentMatches = _matchResults.value
                if (currentMatches.none { it.id == match.id }) {
                    _matchResults.value = currentMatches + match
                }
            }
        }

        val profile = CriteriaManager.getUserProfile(context)
        if (profile == null) {
            return
        }

        val criteriaToAdvertise = CriteriaManager.encodeCriteria(profile.myCriteria)
        val senderId = UserIdManager.getOrGenerateId(context)
        val genderToAdvertise = profile.gender

        // Pass the gender to the advertiser.
        bleAdvertiser.startAdvertising(criteriaToAdvertise, senderId, genderToAdvertise)
        bleScanner.startScanning()
        _isSweeping.value = true
        fetchUserLocation(context)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE])
    fun stop() {
        if (::bleAdvertiser.isInitialized) bleAdvertiser.stopAdvertising()
        if (::bleScanner.isInitialized) bleScanner.stopScanning()

        _isSweeping.value = false
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun fetchUserLocation(context: Context) {
        if (!::locationHelper.isInitialized) locationHelper = LocationHelper(context)

        locationHelper.fetchCurrentLocation { newLocation ->
            val lastLocation = lastZoomLocation
            if (lastLocation == null || lastLocation.distanceTo(newLocation) > 5000) {
                _userLocation.value = newLocation
                lastZoomLocation = newLocation
            }
        }
    }
}