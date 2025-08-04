package com.proxilocal.hyperlocal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID
import kotlin.test.assertFailsWith

class BLEConstantsTest {

    @Test
    fun `SERVICE_UUID is correctly initialized`() {
        val expectedUuid = UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
        assertEquals(expectedUuid, BLEConstants.SERVICE_UUID)
    }

    @Test
    fun `SERVICE_PARCEL_UUID matches SERVICE_UUID`() {
        assertEquals(BLEConstants.SERVICE_UUID, BLEConstants.SERVICE_PARCEL_UUID.uuid)
    }
}