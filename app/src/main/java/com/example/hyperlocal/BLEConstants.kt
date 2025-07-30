package com.example.hyperlocal

import android.os.ParcelUuid
import java.util.UUID

object BLEConstants {
    val SERVICE_UUID: UUID = UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    val SERVICE_PARCEL_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)
}