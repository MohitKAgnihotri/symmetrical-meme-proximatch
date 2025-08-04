package com.proxilocal.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.proxilocal.hyperlocal.CriteriaManager.calculateMatchPercentage
import com.proxilocal.hyperlocal.CriteriaManager.decodeCriteria

class BLEScanner(
    private val context: Context,
    private val onMatchFound: (MatchResult) -> Unit
) {
    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanner = bluetoothAdapter.bluetoothLeScanner
    private var callback: ScanCallback? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        if (!bluetoothAdapter.isEnabled) {
            Log.e("BLEScanner", "Bluetooth is not enabled")
            return
        }
        if (scanner == null) {
            Log.e("BLEScanner", "BLE scanning not supported on this device")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEScanner", "BLUETOOTH_SCAN permission not granted")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEScanner", "ACCESS_FINE_LOCATION permission not granted")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.getServiceData(BLEConstants.SERVICE_PARCEL_UUID)
                if (serviceData == null) {
                    return
                }

                val rssi = result.rssi
                val ourId = String(UserIdManager.getOrGenerateId(context), Charsets.UTF_8)

                if (serviceData.size < 17) {
                    Log.w("BLEScanner", "Invalid service data size: ${serviceData.size} bytes")
                    return
                }

                try {
                    val genderIndex = serviceData[0].toInt()
                    val receivedGender = Gender.values().getOrNull(genderIndex) ?: Gender.PRIVATE
                    val criteriaBytes = serviceData.copyOfRange(1, 9) // 8 bytes for criteria
                    val senderId = serviceData.copyOfRange(9, 17).toString(Charsets.UTF_8) // 8 bytes for ID

                    if (senderId == ourId) {
                        return // Ignore own advertisements
                    }

                    val theirPrefs = decodeCriteria(criteriaBytes)
                    val ourProfile = CriteriaManager.getUserProfile(context)

                    if (ourProfile == null) {
                        Log.w("BLEScanner", "User profile not found")
                        return
                    }

                    val matchPercent = calculateMatchPercentage(ourProfile.theirCriteria, theirPrefs)
                    val matchResult = MatchResult(
                        id = senderId,
                        matchPercentage = matchPercent,
                        distanceRssi = rssi,
                        gender = receivedGender
                    )
                    onMatchFound(matchResult)
                    Log.d("BLEScanner", "Detected $senderId ($receivedGender) @ $rssi dBm → $matchPercent% match")
                } catch (e: Exception) {
                    Log.e("BLEScanner", "Error processing scan result", e)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val errorMessage = when (errorCode) {
                    ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                    ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                    ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                    ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                    else -> "Unknown error"
                }
                Log.e("BLEScanner", "Scan failed: $errorMessage ($errorCode)")
            }
        }

        try {
            scanner?.startScan(null, settings, callback)
            Log.d("BLEScanner", "Started scanning")
        } catch (e: SecurityException) {
            Log.e("BLEScanner", "Security exception during scan start", e)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        if (scanner == null || callback == null) {
            Log.w("BLEScanner", "Scanner or callback is null, cannot stop scanning")
            return
        }
        try {
            scanner?.stopScan(callback)
            Log.d("BLEScanner", "Stopped scanning")
        } catch (e: SecurityException) {
            Log.e("BLEScanner", "Security exception during scan stop", e)
        } finally {
            callback = null
            scanner = null
        }
    }
}