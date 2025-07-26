package com.example.hyperlocal

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.UUID

class BLEAdvertiser(private val context: Context) {

    private val advertiser = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
        .adapter.bluetoothLeAdvertiser

    fun startAdvertising(criteriaData: ByteArray, senderId: ByteArray) {
        val payload = criteriaData + senderId // combine bitmask and user ID

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceData(
                ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")),
                payload
            )
            .setIncludeDeviceName(false)
            .build()

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BLEAdvertiser", "Started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLEAdvertiser", "Advertising failed: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        Log.d("BLEAdvertiser", "Stopped advertising")
    }
}
