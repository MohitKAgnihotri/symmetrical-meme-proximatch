package com.proxilocal.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class BLEAdvertiser(private val context: Context) {

    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val advertiser = bluetoothAdapter.bluetoothLeAdvertiser

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun startAdvertising(criteriaData: ByteArray, senderId: ByteArray, gender: Gender) {
        if (!bluetoothAdapter.isEnabled) {
            Log.e("BLEAdvertiser", "Bluetooth is not enabled")
            return
        }

        if (advertiser == null) {
            Log.e("BLEAdvertiser", "BLE advertising not supported on this device")
            return
        }

        // --- CORRECTED PERMISSION CHECK ---
        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_ADVERTISE
        } else {
            Manifest.permission.BLUETOOTH_ADMIN
        }

        if (ContextCompat.checkSelfPermission(context, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLEAdvertiser", "$requiredPermission permission not granted")
            return
        }
        // --- END OF FIX ---

        val genderByte = gender.ordinal.toByte()
        val payload = byteArrayOf(genderByte) + criteriaData + senderId

        if (payload.size > 31) {
            Log.e("BLEAdvertiser", "Payload exceeds 31 bytes")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceData(BLEConstants.SERVICE_PARCEL_UUID, payload)
            .setIncludeDeviceName(true)
            .build()

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e("BLEAdvertiser", "Security exception on startAdvertising", e)
        }
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
            try {
                advertiser.stopAdvertising(advertiseCallback)
                Log.d("BLEAdvertiser", "Stopped advertising")
            } catch (e: SecurityException) {
                Log.e("BLEAdvertiser", "Security exception on stopAdvertising", e)
            }
        } else {
            Log.e("BLEAdvertiser", "Advertiser is null, cannot stop advertising")
        }
    }
}