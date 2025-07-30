package com.example.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.hyperlocal.CriteriaManager.calculateMatchPercentage
import com.example.hyperlocal.CriteriaManager.decodeCriteria

class BLEScanner(
    private val context: Context,
    private val onMatchFound: (MatchResult) -> Unit
) {
    private var scanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
        .adapter.bluetoothLeScanner
    private var callback: ScanCallback? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(BLEConstants.SERVICE_PARCEL_UUID) // Use the constant here
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.getServiceData(BLEConstants.SERVICE_PARCEL_UUID)
                val rssi = result.rssi
                val ourId = String(UserIdManager.getOrGenerateId(context), Charsets.UTF_8)

                if (serviceData != null && serviceData.size >= 13) { // 1 (gender) + 4 (criteria) + 8 (id)
                    val genderIndex = serviceData[0].toInt()
                    val receivedGender = Gender.values().getOrNull(genderIndex) ?: Gender.PRIVATE

                    val criteriaBytes = serviceData.copyOfRange(1, 5) // Now starts at index 1
                    val senderId = serviceData.copyOfRange(5, 13).toString(Charsets.UTF_8) // And this starts at 5

                    if (senderId != ourId) {
                        val theirPrefs = decodeCriteria(criteriaBytes)
                        val ourProfile = CriteriaManager.getUserProfile(context)

                        if (ourProfile != null) {
                            val matchPercent = calculateMatchPercentage(ourProfile.theirCriteria, theirPrefs)

                            onMatchFound(
                                MatchResult(
                                    id = senderId,
                                    matchPercentage = matchPercent,
                                    distanceRssi = rssi,
                                    gender = receivedGender // Use the parsed gender
                                )
                            )
                            Log.d("BLEScanner", "Detected $senderId ($receivedGender) @ $rssi dBm → $matchPercent% match")
                        }
                    }
                }
            }
        }

        scanner?.startScan(listOf(filter), settings, callback)
        Log.d("BLEScanner", "Started scanning")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        scanner?.stopScan(callback)
        callback = null
        scanner = null
        Log.d("BLEScanner", "Stopped scanning")
    }
}