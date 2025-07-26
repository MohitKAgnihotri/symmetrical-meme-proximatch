package com.example.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
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

            if (serviceData != null && serviceData.size >= 12) {
                val criteriaBytes = serviceData.copyOfRange(0, 4)
                val senderId = serviceData.copyOfRange(4, 12).toString(Charsets.UTF_8)

                val remotePrefs = CriteriaManager.decodeCriteria(criteriaBytes)
                val localPrefs = CriteriaManager.getUserPreferences(context)
                val matchPercentage = CriteriaManager.calculateMatchPercentage(localPrefs, remotePrefs)
                val colorCode = when {
                    matchPercentage >= 75 -> "Green"
                    matchPercentage >= 40 -> "Yellow"
                    else -> "Grey"
                }

                val isMutual = InterestManager.hasSentInterest(context, senderId)

                if (isMutual) {
                    Log.d("BLEScanner", "🎉 Mutual match with $senderId")
                    Toast.makeText(context, "🎉 Mutual match!", Toast.LENGTH_SHORT).show()
                }

                onMatchFound(MatchResult(matchPercentage, colorCode, id = senderId))
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d("BLEScanner", "Stopped scanning")
    }
}
