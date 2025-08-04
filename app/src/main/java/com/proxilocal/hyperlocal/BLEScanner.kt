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

/**
 * Manages Bluetooth Low Energy (BLE) scanning to detect nearby users and calculate match percentages
 * based on their advertised criteria.
 *
 * @param context The application context, used to access Bluetooth services.
 * @param onMatchFound Callback invoked when a matching user is detected, providing a [MatchResult].
 * @param onPermissionMissing Callback invoked when required permissions are missing, allowing UI prompts.
 */
class BLEScanner(
    private val context: Context,
    private val onMatchFound: (MatchResult) -> Unit,
    private val onPermissionMissing: (List<String>) -> Unit
) {
    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanner = bluetoothAdapter.bluetoothLeScanner
    private var callback: ScanCallback? = null
    private val TAG = "BLEScanner"

    /**
     * Starts BLE scanning to detect nearby devices advertising the ProxiLocal service.
     * Requires [Manifest.permission.BLUETOOTH_SCAN] and [Manifest.permission.ACCESS_FINE_LOCATION].
     * Notifies [onPermissionMissing] if permissions are not granted.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        // Reinitialize scanner if null
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

        val missingPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (missingPermissions.isNotEmpty()) {
            Log.e(TAG, "Missing permissions: $missingPermissions")
            onPermissionMissing(missingPermissions)
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.getServiceData(BLEConstants.SERVICE_PARCEL_UUID)
                if (serviceData == null) {
                    Log.d(TAG, "No service data found in scan result")
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
                    val criteriaBytes = serviceData.copyOfRange(1, 9) // 8 bytes for criteria
                    val senderId = serviceData.copyOfRange(9, 17).toString(Charsets.UTF_8) // 8 bytes for ID

                    if (senderId == ourId) {
                        Log.d(TAG, "Ignoring own advertisement")
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
                    Log.d(TAG, "Detected $senderId ($receivedGender) @ ${result.rssi} dBm → $matchPercent% match")
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

    /**
     * Stops BLE scanning and cleans up resources.
     * Requires [Manifest.permission.BLUETOOTH_SCAN].
     */
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
            onPermissionMissing(listOf(Manifest.permission.BLUETOOTH_SCAN))
        } finally {
            callback = null
        }
    }
}