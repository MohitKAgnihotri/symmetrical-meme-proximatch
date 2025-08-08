package com.proxilocal.hyperlocal

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxilocal.flags.FeatureFlags
import com.proxilocal.ui.components.RadarTheme
import com.proxilocal.ui.components.ThemeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private lateinit var bleAdvertiser: BLEAdvertiser
    private lateinit var bleScanner: BLEScanner
    private lateinit var locationHelper: LocationHelper

    // Kill‑switch (unchanged)
    private val _interactiveRadarEnabled = MutableStateFlow(FeatureFlags.enableInteractiveRadar)
    val interactiveRadarEnabled: StateFlow<Boolean> = _interactiveRadarEnabled.asStateFlow()
    fun setInteractiveRadarEnabled(enabled: Boolean) {
        FeatureFlags.enableInteractiveRadar = enabled
        _interactiveRadarEnabled.value = enabled
    }

    // Match state
    private val _matchResultsMap = MutableStateFlow<Map<String, MatchResult>>(emptyMap())
    val matchResults: StateFlow<List<MatchResult>> = _matchResultsMap
        .map { it.values.toList().sortedByDescending { m -> m.matchPercentage } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTheme = MutableStateFlow(ThemeProvider.NeonTech)
    val selectedTheme: StateFlow<RadarTheme> = _selectedTheme

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation

    private val _isSweeping = MutableStateFlow(false)
    val isSweeping: StateFlow<Boolean> = _isSweeping

    private val _permissionError = MutableStateFlow<List<String>?>(null)
    val permissionError: StateFlow<List<String>?> = _permissionError

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    private var lastZoomLocation: Location? = null
    private var cleanupJob: Job? = null

    // RSSI smoothing (kept minimal; we only use initial position now)
    private val smoothedRssi: MutableMap<String, Float> = mutableMapOf()
    private val rssiAlpha = 0.25f

    // Visibility windows
    private val TIMEOUT_MS = 5_000L   // unseen for 5s = start fading
    private val FADE_MS = 1_000L      // fade duration
    private val REMOVE_MS = TIMEOUT_MS + FADE_MS

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun start(context: Context) {
        if (!::bleAdvertiser.isInitialized) bleAdvertiser = BLEAdvertiser(context)
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(
                context = context,
                onMatchFound = { incoming: MatchResult ->
                    val prev = smoothedRssi[incoming.id] ?: incoming.distanceRssi.toFloat()
                    val smoothed = (rssiAlpha * incoming.distanceRssi + (1f - rssiAlpha) * prev)
                    smoothedRssi[incoming.id] = smoothed

                    val currentMap = _matchResultsMap.value.toMutableMap()
                    currentMap[incoming.id] = incoming.copy(
                        distanceRssi = smoothed.toInt(),
                        lastSeen = System.currentTimeMillis()
                    )
                    _matchResultsMap.value = currentMap
                },
                onPermissionMissing = { permissions ->
                    viewModelScope.launch { _permissionError.value = permissions }
                }
            )
        }

        val profile = CriteriaManager.getUserProfile(context)
        val criteriaToAdvertise = CriteriaManager.encodeCriteria(profile.myCriteria)
        val senderId = UserIdManager.getOrGenerateId(context)
        val genderToAdvertise = profile.gender

        bleAdvertiser.startAdvertising(criteriaToAdvertise, senderId, genderToAdvertise)
        bleScanner.startScanning()
        _isSweeping.value = true
        startCleanupJob()
        fetchUserLocation(context)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE])
    fun stop() {
        if (::bleAdvertiser.isInitialized) bleAdvertiser.stopAdvertising()
        if (::bleScanner.isInitialized) bleScanner.stopScanning()
        cleanupJob?.cancel()
        _matchResultsMap.value = emptyMap()
        _isSweeping.value = false
    }

    private fun startCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = viewModelScope.launch {
            while (isActive) {
                delay(250) // tick faster so UI fade looks smooth
                val now = System.currentTimeMillis()
                val updated = _matchResultsMap.value.filterValues { match ->
                    (now - match.lastSeen) < REMOVE_MS
                }
                if (updated.size != _matchResultsMap.value.size) {
                    _matchResultsMap.value = updated
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun fetchUserLocation(context: Context) {
        if (!::locationHelper.isInitialized) locationHelper = LocationHelper(context)
        locationHelper.fetchCurrentLocation(
            onLocationFetched = { newLocation ->
                val lastLocation = lastZoomLocation
                if (lastLocation == null || lastLocation.distanceTo(newLocation) > 5000) {
                    _userLocation.value = newLocation
                    lastZoomLocation = newLocation
                    _locationError.value = null
                }
            },
            onLocationError = { error -> viewModelScope.launch { _locationError.value = error } }
        )
    }

    fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val permissions = _permissionError.value ?: return
        launcher.launch(permissions.toTypedArray())
    }

    fun clearPermissionError() { _permissionError.value = null }
    fun clearLocationError() { _locationError.value = null }
}
