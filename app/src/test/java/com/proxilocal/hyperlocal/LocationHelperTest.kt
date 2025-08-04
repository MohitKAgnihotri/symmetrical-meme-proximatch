package com.proxilocal.hyperlocal

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LocationHelperTest {

    private lateinit var context: Context
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationHelper: LocationHelper
    private lateinit var mockLocation: Location

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        fusedLocationClient = mock(FusedLocationProviderClient::class.java)
        locationHelper = LocationHelper(context)
        mockLocation = mock(Location::class.java)
        `when`(mockLocation.latitude).thenReturn(37.7749)
        `when`(mockLocation.longitude).thenReturn(-122.4194)

        // Use reflection to inject mock FusedLocationProviderClient
        val field = LocationHelper::class.java.getDeclaredField("fusedLocationClient")
        field.isAccessible = true
        field.set(locationHelper, fusedLocationClient)
    }

    @Test
    fun `fetchCurrentLocation calls onLocationFetched with valid location`() {
        `when`(ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION"))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(fusedLocationClient.getCurrentLocation(anyInt(), any(CancellationToken::class.java)))
            .thenReturn(Tasks.forResult(mockLocation))

        var fetchedLocation: Location? = null
        locationHelper.fetchCurrentLocation(
            onLocationFetched = { location -> fetchedLocation = location },
            onLocationError = { error -> throw AssertionError("Unexpected error: $error") }
        )

        assertEquals(mockLocation, fetchedLocation)
    }

    @Test
    fun `fetchCurrentLocation calls onLocationError if permission is missing`() {
        `when`(ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION"))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        var errorMessage: String? = null
        locationHelper.fetchCurrentLocation(
            onLocationFetched = { throw AssertionError("Unexpected location fetched") },
            onLocationError = { error -> errorMessage = error }
        )

        assertEquals("Location permission not granted", errorMessage)
    }

    @Test
    fun `fetchCurrentLocation calls onLocationError if location is null`() {
        `when`(ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION"))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(fusedLocationClient.getCurrentLocation(anyInt(), any(CancellationToken::class.java)))
            .thenReturn(Tasks.forResult(null))

        var errorMessage: String? = null
        locationHelper.fetchCurrentLocation(
            onLocationFetched = { throw AssertionError("Unexpected location fetched") },
            onLocationError = { error -> errorMessage = error }
        )

        assertEquals("No location available", errorMessage)
    }

    @Test
    fun `fetchCurrentLocation calls onLocationError on failure`() {
        `when`(ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION"))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(fusedLocationClient.getCurrentLocation(anyInt(), any(CancellationToken::class.java)))
            .thenReturn(Tasks.forException(Exception("Location service error")))

        var errorMessage: String? = null
        locationHelper.fetchCurrentLocation(
            onLocationFetched = { throw AssertionError("Unexpected location fetched") },
            onLocationError = { error -> errorMessage = error }
        )

        assertEquals("Failed to fetch location: Location service error", errorMessage)
    }
}