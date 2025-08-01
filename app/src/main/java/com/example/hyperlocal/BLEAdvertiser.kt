package com.example.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class BLEAdvertiser(private val context: Context) {

    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val advertiser = bluetoothAdapter.bluetoothLeAdvertiser

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising(criteriaData: ByteArray, senderId: ByteArray, gender: Gender) {
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            Log.e("BLEAdvertiser", "Bluetooth is not enabled")
            return
        }

        // Check if advertiser is available
        if (advertiser == null) {
            Log.e("BLEAdvertiser", "BLE advertising not supported on this device")
            return
        }

        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEAdvertiser", "BLUETOOTH_ADVERTISE permission not granted")
            return
        }

        // Check location permission (required for BLE advertising)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEAdvertiser", "ACCESS_FINE_LOCATION permission not granted")
            return
        }

        // Prepare payload
        val genderByte = gender.ordinal.toByte()
        val payload = byteArrayOf(genderByte) + criteriaData + senderId
        Log.d("BLEAdvertiser", "Payload size: ${payload.size} bytes")

        // Check payload size
        if (payload.size > 31) {
            Log.e("BLEAdvertiser", "Payload exceeds 31 bytes")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER) // Changed to LOW_POWER to reduce battery usage
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM) // Changed to MEDIUM for compatibility
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceData(BLEConstants.SERVICE_PARCEL_UUID, payload)
            .setIncludeDeviceName(true)
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BLEAdvertiser", "Started advertising with settings: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            val errorMessage = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error"
            }
            Log.e("BLEAdvertiser", "Advertising failed: $errorMessage ($errorCode)")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        if (advertiser != null) {
            advertiser.stopAdvertising(advertiseCallback)
            Log.d("BLEAdvertiser", "Stopped advertising")
        } else {
            Log.e("BLEAdvertiser", "Advertiser is null, cannot stop advertising")
        }
    }
}