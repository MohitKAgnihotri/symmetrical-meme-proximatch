package com.proxilocal.hyperlocal

import android.Manifest
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proxilocal.flags.FeatureFlags
import com.proxilocal.ui.components.RadarTheme
import com.proxilocal.ui.components.ThemeProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
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

    // Map of id -> MatchResult (raw)
    private val _matchResultsMap = MutableStateFlow<Map<String, MatchResult>>(emptyMap())

    // In-memory per-id UI state (status/likeType)
    private val matchUi = mutableMapOf<String, MatchUiState>()

    // UI observes sorted raw list (for list)
    val matchResults: StateFlow<List<MatchResult>> = _matchResultsMap
        .map { it.values.toList().sortedByDescending { m -> m.matchPercentage } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI observes wrapped states (for radar)
    val matchUiList: StateFlow<List<MatchUiState>> = _matchResultsMap
        .map { map ->
            map.values
                .map { m -> matchUi[m.id]?.copy(match = m) ?: MatchUiState(match = m) }
                .sortedByDescending { it.match.matchPercentage }
        }
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

    // RSSI smoothing (EMA)
    private val smoothedRssi = mutableMapOf<String, Float>()
    private val rssiAlpha = 0.30f // 0..1, higher = more reactive

    /* ───────────── One-shot UI events (toasts / navigation) ───────────── */
    sealed class UiEvent {
        data class ToastMsg(val msg: String) : UiEvent()
        data class NavigateToChat(val peerId: String) : UiEvent()
    }
    val events = MutableSharedFlow<UiEvent>()

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
                    val updated = incoming.copy(distanceRssi = smoothed.toInt(), lastSeen = System.currentTimeMillis())
                    current[updated.id] = updated
                    _matchResultsMap.value = current

                    // Ensure a UI state entry exists
                    matchUi.putIfAbsent(updated.id, MatchUiState(match = updated))
                },
                onPermissionMissing = { permissions ->
                    viewModelScope.launch { _permissionError.value = permissions }
                },
                onInboundInteraction = { senderIdHex, opcode ->
                    handleInboundLike(senderIdHex, opcode)
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
        matchUi.clear()
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
                    matchUi.keys.retainAll(updated.keys) // prune UI state
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

    /* ───── Phase 4/5: one‑sided "send like" + mutual detection ───── */

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun sendLikeTo(targetIdHex: String, type: LikeType) {
        if (!::bleAdvertiser.isInitialized || !::appContext.isInitialized) return
        try {
            val myId = UserIdManager.getOrGenerateId(appContext) // 8 bytes
            val targetId = hexToBytes(targetIdHex)                // 8 bytes (from UI)
            bleAdvertiser.sendLike(myId = myId, targetId = targetId, type = type)

            // Persist "we liked them" and reflect UI state
            InterestManager.saveInterest(appContext, targetIdHex)
            val base = _matchResultsMap.value[targetIdHex] ?: return
            val cur = matchUi[targetIdHex]
            matchUi[targetIdHex] = (cur ?: MatchUiState(base)).copy(
                status = if (type == LikeType.SUPER_LIKE) MatchStatus.SUPER_LIKED else MatchStatus.LIKED,
                likeType = type
            )
            // nudge observers
            _matchResultsMap.value = _matchResultsMap.value.toMap()
        } catch (t: Throwable) {
            Log.e("MainViewModel", "sendLikeTo failed for $targetIdHex", t)
        }
    }

    // Inbound LIKE/SUPER_LIKE targeted to me
    private fun handleInboundLike(senderIdHex: String, @Suppress("UNUSED_PARAMETER") opcode: Byte) {
        if (!FeatureFlags.enableMutualTransition) return
        val weLiked = InterestManager.hasSentInterest(appContext, senderIdHex)
        if (weLiked) {
            markMutual(senderIdHex)
        }
    }

    private fun markMutual(peerId: String) {
        val base = _matchResultsMap.value[peerId] ?: return
        val cur = matchUi[peerId]
        matchUi[peerId] = (cur ?: MatchUiState(base)).copy(status = MatchStatus.MUTUAL)
        _matchResultsMap.value = _matchResultsMap.value.toMap()
        Log.d("MainViewModel", "Marked MUTUAL with $peerId")
    }

    /* ───── Phase 6: rings tap → connect (stub) ───── */

    fun isMutual(id: String): Boolean = (matchUi[id]?.status == MatchStatus.MUTUAL)
    fun isConnected(id: String): Boolean = (matchUi[id]?.status == MatchStatus.CONNECTED)

    fun onRingsTapped(peerId: String) {
        if (!FeatureFlags.enableConnectFromRings) {
            viewModelScope.launch { events.emit(UiEvent.ToastMsg("Rings are visual only (disabled).")) }
            return
        }
        if (!isMutual(peerId)) {
            viewModelScope.launch { events.emit(UiEvent.ToastMsg("Not mutual yet.")) }
            return
        }
        viewModelScope.launch {
            events.emit(UiEvent.ToastMsg("Connecting to ${peerId.take(6)}…"))
            val ok = SecureConnector.startHandshake(peerId)
            if (ok) {
                val base = _matchResultsMap.value[peerId] ?: return@launch
                val cur = matchUi[peerId]
                matchUi[peerId] = (cur ?: MatchUiState(base)).copy(status = MatchStatus.CONNECTED)
                _matchResultsMap.value = _matchResultsMap.value.toMap()
                events.emit(UiEvent.ToastMsg("Connected with ${peerId.take(6)}"))
                events.emit(UiEvent.NavigateToChat(peerId))
            } else {
                events.emit(UiEvent.ToastMsg("Connect failed. Try again."))
            }
        }
    }

    private object SecureConnector {
        suspend fun startHandshake(@Suppress("UNUSED_PARAMETER") peerId: String): Boolean {
            delay(600) // pretend to do key exchange
            return true
        }
    }

    /* ───── Utils ───── */

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.lowercase().trim()
        require(clean.length % 2 == 0) { "Invalid hex length" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
