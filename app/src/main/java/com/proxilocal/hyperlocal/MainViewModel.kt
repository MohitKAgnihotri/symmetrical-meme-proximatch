package com.proxilocal.hyperlocal

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    // 1. CHANGE HOW MATCHES ARE STORED
    // Instead of a simple list, we use a map where the key is the device ID.
    // This makes it easy to update a device's timestamp when it's seen again.
    private val _matchResultsMap = MutableStateFlow<Map<String, MatchResult>>(emptyMap())

    // The UI will still observe a simple list, which we derive from our map.
    // We also sort the list here for a better user experience.
    val matchResults: StateFlow<List<MatchResult>> = _matchResultsMap
        .map { it.values.toList().sortedByDescending { match -> match.matchPercentage } }
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
    // 2. ADD A JOB FOR THE CLEANUP TASK
    private var cleanupJob: Job? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun start(context: Context) {
        if (!::bleAdvertiser.isInitialized) bleAdvertiser = BLEAdvertiser(context)
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(
                context = context,
                // 3. UPDATE THE onMatchFound LAMBDA
                // This now adds or updates an entry in our map.
                onMatchFound = { match: MatchResult ->
                    val currentMap = _matchResultsMap.value.toMutableMap()
                    currentMap[match.id] = match
                    _matchResultsMap.value = currentMap
                },
                onPermissionMissing = { permissions ->
                    viewModelScope.launch {
                        _permissionError.value = permissions
                    }
                }
            )
        }

        val profile = CriteriaManager.getUserProfile(context)
        if (profile == null) {
            return
        }

        val criteriaToAdvertise = CriteriaManager.encodeCriteria(profile.myCriteria)
        val senderId = UserIdManager.getOrGenerateId(context)
        val genderToAdvertise = profile.gender

        bleAdvertiser.startAdvertising(criteriaToAdvertise, senderId, genderToAdvertise)
        bleScanner.startScanning()
        _isSweeping.value = true
        // 4. START THE CLEANUP JOB WHEN SCANNING STARTS
        startCleanupJob()
        fetchUserLocation(context)
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE])
    fun stop() {
        if (::bleAdvertiser.isInitialized) bleAdvertiser.stopAdvertising()
        if (::bleScanner.isInitialized) bleScanner.stopScanning()

        // 5. STOP THE CLEANUP JOB AND CLEAR THE LIST
        cleanupJob?.cancel()
        _matchResultsMap.value = emptyMap()
        _isSweeping.value = false
    }

    // 6. ADD THE NEW CLEANUP FUNCTION
    /**
     * Periodically checks the match list and removes any that haven't been seen
     * within the timeout period.
     */
    private fun startCleanupJob() {
        cleanupJob?.cancel() // Cancel any existing job
        cleanupJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // Run this check every second
                val now = System.currentTimeMillis()
                val timeout = 1000 // 1 second timeout

                // Filter the map, keeping only the matches seen within the last second
                val updatedMap = _matchResultsMap.value.filterValues { match ->
                    (now - match.lastSeen) < timeout
                }

                // Only update the state if there was a change, to avoid unnecessary recompositions
                if (updatedMap.size != _matchResultsMap.value.size) {
                    _matchResultsMap.value = updatedMap
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
            onLocationError = { error ->
                viewModelScope.launch {
                    _locationError.value = error
                }
            }
        )
    }

    fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val permissions = _permissionError.value ?: return
        launcher.launch(permissions.toTypedArray())
    }

    fun clearPermissionError() {
        _permissionError.value = null
    }

    fun clearLocationError() {
        _locationError.value = null
    }
}
