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
    private lateinit var appContext: Context // keep for later sendLike calls

    // Map of id -> MatchResult
    private val _matchResultsMap = MutableStateFlow<Map<String, MatchResult>>(emptyMap())

    // UI observes sorted list
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
        appContext = context.applicationContext

        if (!::bleAdvertiser.isInitialized) bleAdvertiser = BLEAdvertiser(appContext)
        if (!::bleScanner.isInitialized) {
            bleScanner = BLEScanner(
                context = appContext,
                onMatchFound = { incoming: MatchResult ->
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
        val profile = CriteriaManager.getUserProfile(appContext)
        val criteriaToAdvertise = CriteriaManager.encodeCriteria(profile.myCriteria) // 8 bytes
        val senderIdBytes: ByteArray = UserIdManager.getOrGenerateId(appContext)   // 8 bytes
        val genderToAdvertise = profile.gender

        bleAdvertiser.startAdvertising(criteriaToAdvertise, senderIdBytes, genderToAdvertise)
        bleScanner.startScanning()

        _isSweeping.value = true
        startCleanupJob()
        fetchUserLocation(appContext)
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

    private fun startCleanupJob() {
        cleanupJob?.cancel()
        cleanupJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val timeoutMs = 1000L
                val updated = _matchResultsMap.value.filterValues { match ->
                    (now - match.lastSeen) < timeoutMs
                }
                if (updated.size != _matchResultsMap.value.size) {
                    _matchResultsMap.value = updated
                    smoothedRssi.keys.retainAll(updated.keys)
                }
            }
        }
    }

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

    /* ───── Phase 4: one‑sided "send like" wiring ───── */

    /**
     * Send a LIKE/SUPER_LIKE to a target whose id is our UI hex string.
     * Does not perform any mutual detection (Phase 5 handles that).
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun sendLikeTo(targetIdHex: String, type: LikeType) {
        if (!::bleAdvertiser.isInitialized || !::appContext.isInitialized) return
        try {
            val myId = UserIdManager.getOrGenerateId(appContext) // 8 bytes
            val targetId = hexToBytes(targetIdHex)                // 8 bytes (from UI)
            bleAdvertiser.sendLike(myId = myId, targetId = targetId, type = type)
            android.util.Log.d("MainViewModel", "sendLikeTo -> $type $targetIdHex")
        } catch (t: Throwable) {
            // Be noisy in logs but don't crash UI
            android.util.Log.e("MainViewModel", "sendLikeTo failed for $targetIdHex", t)
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.lowercase().trim()
        require(clean.length % 2 == 0) { "Invalid hex length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
