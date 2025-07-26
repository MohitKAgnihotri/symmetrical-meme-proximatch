package com.example.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.*

class BLEScanner(
    private val context: Context,
    private val onMatchFound: (MatchResult) -> Unit
) {
    private val bluetoothLeScanner: BluetoothLeScanner? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val serviceData = result.scanRecord
                ?.getServiceData(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))

            if (serviceData != null) {
                val remoteCriteria = CriteriaManager.decodeCriteria(serviceData)
                val localPreferences = CriteriaManager.getUserPreferences(context)
                val matchPercentage = CriteriaManager.calculateMatchPercentage(localPreferences, remoteCriteria)
                val colorCode = when {
                    matchPercentage >= 75 -> "Green"
                    matchPercentage >= 40 -> "Yellow"
                    else -> "Grey"
                }
                onMatchFound(MatchResult(matchPercentage, colorCode))
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLEScanner", "Scan failed: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
        Log.d("BLEScanner", "Started scanning")
    }

    fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d("BLEScanner", "Stopped scanning")
    }
}
