package com.proxilocal.hyperlocal

import android.os.ParcelUuid
import java.util.UUID

object BLEConstants {
    // Keep your existing UUID the same as before.
    private const val DEFAULT_SERVICE_UUID = "0000FEAA-0000-1000-8000-00805F9B34FB"

    // Some projects have this named SERVICE_PARCEL_UUID; keep both for safety.
    val SERVICE_UUID: UUID = UUID.fromString(DEFAULT_SERVICE_UUID)
    val PARCEL_SERVICE_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)
    val SERVICE_PARCEL_UUID: ParcelUuid = PARCEL_SERVICE_UUID

    // Phase 3 opcodes
    const val OPCODE_LIKE: Byte        = 0x01
    const val OPCODE_SUPER_LIKE: Byte  = 0x02
    const val OPCODE_MUTUAL: Byte      = 0x10
    const val OPCODE_CONNECT_REQ: Byte = 0x20
}
