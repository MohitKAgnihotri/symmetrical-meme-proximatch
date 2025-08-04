package com.proxilocal.hyperlocal

import android.os.ParcelUuid
import java.util.UUID

object BLEConstants {
    /**
     * The UUID used for the BLE service in the ProxiLocal app.
     * This identifies the service for advertising and scanning.
     */
    private const val DEFAULT_SERVICE_UUID = "0000FEAA-0000-1000-8000-00805F9B34FB"

    /**
     * The UUID for the BLE service, validated and parsed from a string.
     */
    val SERVICE_UUID: UUID = createServiceUuid()

    /**
     * The ParcelUuid representation of the BLE service UUID, used for Android BLE operations.
     */
    val SERVICE_PARCEL_UUID: ParcelUuid = ParcelUuid(SERVICE_UUID)

    /**
     * Creates and validates the service UUID.
     * @param uuidString The UUID string to parse (defaults to [DEFAULT_SERVICE_UUID]).
     * @return A valid [UUID] instance.
     * @throws IllegalArgumentException if the UUID string is malformed.
     */
    private fun createServiceUuid(uuidString: String = DEFAULT_SERVICE_UUID): UUID {
        return try {
            UUID.fromString(uuidString)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID format: $uuidString", e)
        }
    }
}

