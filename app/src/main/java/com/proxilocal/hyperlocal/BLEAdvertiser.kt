package com.proxilocal.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class BLEAdvertiser(private val context: Context) {

    private val TAG = "BLEAdvertiser"
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val advertiser = bluetoothManager?.adapter?.bluetoothLeAdvertiser

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertising started: $settingsInEffect")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed with error code: $errorCode")
        }
    }

    private fun hasAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Continuous service-data advertise (your existing beacon).
     * Format: [gender(1)] + [criteria(8)] + [senderId(8)] = 17 bytes
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising(criteriaBytes: ByteArray, senderId: ByteArray, gender: Gender) {
        if (!hasAdvertisePermission()) {
            Log.e(TAG, "Missing BLUETOOTH_ADVERTISE permission."); return
        }
        if (advertiser == null) {
            Log.e(TAG, "BLE advertiser not available."); return
        }

        require(criteriaBytes.size == 8) { "criteriaBytes must be 8 bytes" }
        require(senderId.size == 8) { "senderId must be 8 bytes" }

        val servicePayload = ByteArray(1 + 8 + 8).apply {
            this[0] = gender.ordinal.toByte()
            System.arraycopy(criteriaBytes, 0, this, 1, 8)
            System.arraycopy(senderId, 0, this, 9, 8)
        }

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(BLEConstants.PARCEL_SERVICE_UUID)
            .addServiceData(BLEConstants.PARCEL_SERVICE_UUID, servicePayload)
            .build()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback)
            Log.d(TAG, "Started advertising: serviceData=${servicePayload.size} bytes")

            // Shadow-prep logs (Phase 3)
            val likePayload = ProxiPayloads.encode(BLEConstants.OPCODE_LIKE, senderId)
            val sLikePayload = ProxiPayloads.encode(BLEConstants.OPCODE_SUPER_LIKE, senderId)
            ProxiPayloads.log("AD-PREP like", likePayload)
            ProxiPayloads.log("AD-PREP sLike", sLikePayload)
        } catch (se: SecurityException) {
            Log.e(TAG, "Security exception on startAdvertising", se)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        if (advertiser == null) {
            Log.w(TAG, "BLE advertiser is null, cannot stop"); return
        }
        try {
            advertiser.stopAdvertising(advertiseCallback)
            Log.d(TAG, "Stopped advertising")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception on stopAdvertising", e)
        }
    }

    /**
     * Phase 4: burst out a LIKE / SUPER_LIKE intent via manufacturer data.
     * We start a short-lived advertise and auto-stop after a small delay
     * to avoid interfering with your continuous service-data beacon.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun sendLike(myId: ByteArray, targetId: ByteArray, type: LikeType) {
        if (!hasAdvertisePermission()) {
            Log.e(TAG, "Missing BLUETOOTH_ADVERTISE permission for sendLike()."); return
        }
        if (advertiser == null) {
            Log.e(TAG, "BLE advertiser not available for sendLike()."); return
        }
        if (myId.size != ProxiPayloads.ID_BYTES || targetId.size != ProxiPayloads.ID_BYTES) {
            Log.e(TAG, "Invalid id sizes for sendLike()"); return
        }

        val opcode = when (type) {
            LikeType.LIKE -> BLEConstants.OPCODE_LIKE
            LikeType.SUPER_LIKE -> BLEConstants.OPCODE_SUPER_LIKE
        }
        val payload = ProxiPayloads.encode(opcode, myId, targetId)
        ProxiPayloads.log("TX like burst", payload)

        // Use a reserved/test company id for dev (0xFFFF). Adjust as needed.
        val burstData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(0xFFFF, payload)
            .build()

        val burstSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val burstCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i(TAG, "sendLike burst started")
            }
            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "sendLike burst failed: $errorCode")
            }
        }

        try {
            advertiser.startAdvertising(burstSettings, burstData, burstCallback)
            // Auto stop after ~600ms to keep the air clean
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    advertiser.stopAdvertising(burstCallback)
                    Log.i(TAG, "sendLike burst stopped")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error stopping like burst", e)
                }
            }, 600L)
        } catch (se: SecurityException) {
            Log.e(TAG, "Security exception on sendLike()", se)
        }
    }
}
