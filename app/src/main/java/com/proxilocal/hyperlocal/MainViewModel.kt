package com.proxilocal.hyperlocal

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxilocal.ui.components.RadarTheme
import com.proxilocal.ui.components.ThemeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private lateinit var bleAdvertiser: BLEAdvertiser
    private lateinit var bleScanner: BLEScanner
    private lateinit var locationHelper: LocationHelper

    // Keep a map keyed by device id (hex) so repeated sightings update the same entry.
    private val _matchResultsMap = MutableStateFlow<Map<String, MatchResult>>(emptyMap())

    // The UI observes a sorted list derived from the map.
    val matchResults: StateFlow<List<MatchResult>> = _matchResultsMap
        .map { it.values.toList().sortedByDescending { m -> m.matchPercentage } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    // RSSI smoothing (EMA)
    private val smoothedRssi = mutableMapOf<String, Float>()
    private val rssiAlpha = 0.30f // 0..1, higher = more reactive

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun start(context: Context) {
        if (!::bleAdvertiser.isInitialized) bleAdvertiser = BLEAdvertiser(context)
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(
                context = context,
                onMatchFound = { incoming: MatchResult ->
                    // Smooth RSSI and refresh lastSeen
                    val prev = smoothedRssi[incoming.id] ?: incoming.distanceRssi.toFloat()
                    val smoothed = (rssiAlpha * incoming.distanceRssi) + ((1f - rssiAlpha) * prev)
                    smoothedRssi[incoming.id] = smoothed

                    val current = _matchResultsMap.value.toMutableMap()
                    current[incoming.id] = incoming.copy(
                        distanceRssi = smoothed.toInt(),
                        lastSeen = System.currentTimeMillis()
                    )
                    _matchResultsMap.value = current
                },
                onPermissionMissing = { permissions ->
                    viewModelScope.launch { _permissionError.value = permissions }
                }
            )
        }

        // Load profile and start advertising + scanning
        val profile = CriteriaManager.getUserProfile(context)
        val criteriaToAdvertise = CriteriaManager.encodeCriteria(profile.myCriteria) // 8 bytes
        val senderIdBytes: ByteArray = UserIdManager.getOrGenerateId(context)       // 8 bytes
        val genderToAdvertise = profile.gender

        bleAdvertiser.startAdvertising(criteriaToAdvertise, senderIdBytes, genderToAdvertise)
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
        smoothedRssi.clear()
        _isSweeping.value = false
    }

    /**
     * Periodically remove matches that haven't been seen recently.
     * Keep this short so dots "gracefully" disappear when a device leaves.
     */
    private fun startCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // run every second
                val now = System.currentTimeMillis()
                val timeoutMs = 1000L // seen within the last second
                val updated = _matchResultsMap.value.filterValues { match ->
                    (now - match.lastSeen) < timeoutMs
                }
                if (updated.size != _matchResultsMap.value.size) {
                    _matchResultsMap.value = updated
                    // also prune the rssi cache
                    smoothedRssi.keys.retainAll(updated.keys)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun fetchUserLocation(context: Context) {
        if (!::locationHelper.isInitialized) locationHelper = LocationHelper(context)
        locationHelper.fetchCurrentLocation(
            onLocationFetched = { newLocation ->
                val last = lastZoomLocation
                if (last == null || last.distanceTo(newLocation) > 5000) {
                    _userLocation.value = newLocation
                    lastZoomLocation = newLocation
                    _locationError.value = null
                }
            },
            onLocationError = { error ->
                viewModelScope.launch { _locationError.value = error }
            }
        )
    }

    fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val permissions = _permissionError.value ?: return
        launcher.launch(permissions.toTypedArray())
    }

    fun clearPermissionError() { _permissionError.value = null }
    fun clearLocationError() { _locationError.value = null }
}
