package com.proxilocal.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
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
    private val TAG = "BLEScanner"
    private val bluetoothAdapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanner = bluetoothAdapter.bluetoothLeScanner
    private var callback: ScanCallback? = null

    private fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    private fun hasScanPermission(): Boolean =
        requiredPermissions().all { p ->
            ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        if (scanner == null) scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) { Log.e(TAG, "BLE scanning not supported"); return }
        if (!bluetoothAdapter.isEnabled) { Log.e(TAG, "Bluetooth not enabled"); return }

        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) { onPermissionMissing(missing); return }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Filter for our Service UUID; any service data length matches.
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(BLEConstants.SERVICE_PARCEL_UUID)
                .build()
        )

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = handleResult(result)
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { handleResult(it) }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed: $errorCode")
            }
        }

        try {
            scanner!!.startScan(filters, settings, callback)
            Log.d(TAG, "Started scanning with filter ${BLEConstants.SERVICE_UUID}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during scan start", e)
            onPermissionMissing(listOf(Manifest.permission.BLUETOOTH_SCAN))
        }
    }

    private fun handleResult(result: ScanResult) {
        val record = result.scanRecord ?: return

        // ─── Phase 3 shadow parse of manufacturer data ───
        try {
            val mfd = record.manufacturerSpecificData
            if (mfd != null && mfd.size() > 0) {
                for (i in 0 until mfd.size()) {
                    val companyId = mfd.keyAt(i)
                    val data = mfd.valueAt(i)
                    ProxiPayloads.decode(data)?.let { msg ->
                        when (msg.opcode) {
                            BLEConstants.OPCODE_LIKE ->
                                Log.i(TAG, "SHADOW RX: LIKE from ${msg.senderId.toHex()} to ${msg.targetId.toHex()} (companyId=$companyId)")
                            BLEConstants.OPCODE_SUPER_LIKE ->
                                Log.i(TAG, "SHADOW RX: SUPER_LIKE from ${msg.senderId.toHex()} to ${msg.targetId.toHex()} (companyId=$companyId)")
                            BLEConstants.OPCODE_MUTUAL ->
                                Log.i(TAG, "SHADOW RX: MUTUAL from ${msg.senderId.toHex()} to ${msg.targetId.toHex()} (companyId=$companyId)")
                            BLEConstants.OPCODE_CONNECT_REQ ->
                                Log.i(TAG, "SHADOW RX: CONNECT_REQ from ${msg.senderId.toHex()} to ${msg.targetId.toHex()} (companyId=$companyId)")
                            else ->
                                Log.d(TAG, "SHADOW RX: unknown opcode ${(msg.opcode.toInt() and 0xFF)}")
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Shadow parse failed", t)
        }

        // ─── Your existing service data path (bytes all the way) ───
        val serviceData = record.getServiceData(BLEConstants.SERVICE_PARCEL_UUID) ?: return

        // Our format: [gender(1)] + [criteria(8)] + [senderId(8)] = 17 bytes
        if (serviceData.size != 17) {
            Log.w(TAG, "Invalid service data size: ${serviceData.size} (expected 17)")
            return
        }

        try {
            val ourId = UserIdManager.getOrGenerateId(context) // ByteArray (8)
            val genderIndex = serviceData[0].toInt()
            val receivedGender = Gender.values().getOrNull(genderIndex) ?: Gender.PRIVATE

            val criteriaBytes = serviceData.copyOfRange(1, 9)     // 8 bytes
            val senderId = serviceData.copyOfRange(9, 17)         // 8 bytes

            // Ignore our own packets
            if (senderId.contentEquals(ourId)) return

            val theirPrefs = decodeCriteria(criteriaBytes)        // List<Boolean>
            val ourProfile = CriteriaManager.getUserProfile(context)
            val matchPercent = calculateMatchPercentage(ourProfile.theirCriteria, theirPrefs)

            // 🔹 Convert bytes -> hex ONLY for UI id (keeps the rest of the app unchanged)
            val matchResult = MatchResult(
                id = senderId.toHex(),
                matchPercentage = matchPercent,
                distanceRssi = result.rssi,
                gender = receivedGender
            )
            onMatchFound(matchResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scan result", e)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        val s = scanner
        val cb = callback
        if (s == null || cb == null) { Log.w(TAG, "Scanner/callback null"); return }
        try {
            s.stopScan(cb)
            Log.d(TAG, "Stopped scanning")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during stopScan", e)
        } finally {
            callback = null
        }
    }
}
