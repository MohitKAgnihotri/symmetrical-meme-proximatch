package com.example.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.hyperlocal.CriteriaManager.calculateMatchPercentage
import com.example.hyperlocal.CriteriaManager.decodeCriteria
import java.util.*

class BLEScanner(
    private val context: Context,
    private val onMatchFound: (MatchResult) -> Unit
) {
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (!bluetoothAdapter.isEnabled) return

        scanner = bluetoothAdapter.bluetoothLeScanner

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord
                    ?.getServiceData(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")))

                val rssi = result.rssi
                val ourId = String(UserIdManager.getOrGenerateId(context), Charsets.UTF_8)

                if (serviceData != null && serviceData.size >= 12) {
                    val criteriaBytes = serviceData.copyOfRange(0, 4)
                    val senderId = serviceData.copyOfRange(4, 12).toString(Charsets.UTF_8)

                    if (senderId != ourId) {
                        val theirPrefs = decodeCriteria(criteriaBytes)

                        // --- FIX: Use the new getUserProfile function ---
                        val ourProfile = CriteriaManager.getUserProfile(context)

                        if (ourProfile != null) {
                            // Use the criteria we're looking for in others
                            val matchPercent = calculateMatchPercentage(ourProfile.theirCriteria, theirPrefs)

                            onMatchFound(
                                MatchResult(
                                    id = senderId,
                                    matchPercentage = matchPercent,
                                    distanceRssi = rssi,
                                    // TODO: Update BLE service data to include a byte for gender
                                    gender = Gender.PRIVATE
                                )
                            )
                            Log.d("BLEScanner", "Detected $senderId @ $rssi dBm → $matchPercent% match")
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