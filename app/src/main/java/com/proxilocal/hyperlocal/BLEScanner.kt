package com.proxilocal.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.proxilocal.hyperlocal.CriteriaManager.calculateMatchPercentage
import com.proxilocal.hyperlocal.CriteriaManager.decodeCriteria

class BLEScanner(
    private val context: Context,
    private val onMatchFound: (MatchResult) -> Unit,
    private val onPermissionMissing: (List<String>) -> Unit
) {
    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanner = bluetoothAdapter.bluetoothLeScanner
    private var callback: ScanCallback? = null
    private val TAG = "BLEScanner"

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        if (scanner == null) {
            scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                Log.e(TAG, "BLE scanning not supported on this device")
                return
            }
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        // --- CORRECTED PERMISSION CHECK ---
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.e(TAG, "Missing permissions for scanning: $missingPermissions")
            onPermissionMissing(missingPermissions)
            return
        }
        // --- END OF FIX ---

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.getServiceData(BLEConstants.SERVICE_PARCEL_UUID)
                if (serviceData == null) {
                    return
                }

                if (serviceData.size != 17) {
                    Log.w(TAG, "Invalid service data size: ${serviceData.size} bytes, expected 17")
                    return
                }

                try {
                    val ourId = String(UserIdManager.getOrGenerateId(context), Charsets.UTF_8)
                    val genderIndex = serviceData[0].toInt()
                    val receivedGender = Gender.values().getOrNull(genderIndex) ?: Gender.PRIVATE
                    val criteriaBytes = serviceData.copyOfRange(1, 9)
                    val senderId = serviceData.copyOfRange(9, 17).toString(Charsets.UTF_8)

                    if (senderId == ourId) {
                        return
                    }

                    val theirPrefs = decodeCriteria(criteriaBytes)
                    val ourProfile = CriteriaManager.getUserProfile(context)
                    val matchPercent = calculateMatchPercentage(ourProfile.theirCriteria, theirPrefs)
                    val matchResult = MatchResult(
                        id = senderId,
                        matchPercentage = matchPercent,
                        distanceRssi = result.rssi,
                        gender = receivedGender
                    )
                    onMatchFound(matchResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing scan result", e)
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
                Log.e(TAG, "Scan failed: $errorMessage ($errorCode)")
            }
        }

        try {
            scanner.startScan(null, settings, callback)
            Log.d(TAG, "Started scanning")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during scan start", e)
            onPermissionMissing(listOf(Manifest.permission.BLUETOOTH_SCAN))
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        if (scanner == null || callback == null) {
            Log.w(TAG, "Scanner or callback is null, cannot stop scanning")
            return
        }
        try {
            scanner.stopScan(callback)
            Log.d(TAG, "Stopped scanning")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during scan stop", e)
        } finally {
            callback = null
        }
    }
}