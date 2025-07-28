// MainViewModel.kt
package com.example.hyperlocal

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import com.example.ui.components.RadarTheme
import com.example.ui.components.ThemeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private lateinit var bleAdvertiser: BLEAdvertiser
    private lateinit var bleScanner: BLEScanner

    // 1️⃣ Flow of match results
    private val _matchResults = MutableStateFlow<List<MatchResult>>(emptyList())
    val matchResults: StateFlow<List<MatchResult>> = _matchResults

    // 2️⃣ Currently selected radar theme
    private val _selectedTheme = MutableStateFlow<RadarTheme>(ThemeProvider.CorporatePulse)
    val selectedTheme: StateFlow<RadarTheme> = _selectedTheme

    /** Switch between available radar themes at runtime */
    fun selectTheme(theme: RadarTheme) {
        _selectedTheme.value = theme
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun start(context: Context) {
        bleAdvertiser = BLEAdvertiser(context)
        bleScanner = BLEScanner(context) { match ->
            // Append new match to the list
            val updated = _matchResults.value.toMutableList().apply { add(match) }
            _matchResults.value = updated
        }

        val criteria = CriteriaManager.getEncodedCriteria(context)
        val senderId = UserIdManager.getOrGenerateId(context)
        bleAdvertiser.startAdvertising(criteria, senderId)
        bleScanner.startScanning()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stop() {
        bleAdvertiser.stopAdvertising()
        bleScanner.stopScanning()
        _matchResults.value = emptyList()
    }
}
