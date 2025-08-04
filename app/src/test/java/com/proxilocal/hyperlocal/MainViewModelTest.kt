package com.proxilocal.hyperlocal

import android.content.Context
import android.location.Location
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var context: Context
    private lateinit var bleScanner: BLEScanner
    private lateinit var bleAdvertiser: BLEAdvertiser
    private lateinit var locationHelper: LocationHelper
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined) // For coroutine testing
        context = mock()
        bleScanner = mock(lenient = true)
        bleAdvertiser = mock(lenient = true)
        locationHelper = mock(lenient = true)
        permissionLauncher = mock()

        // Mock dependencies
        whenever(UserIdManager.getOrGenerateId(context)).thenReturn("testUserId".toByteArray())
        whenever(CriteriaManager.getUserProfile(context)).thenReturn(
            UserProfile(
                gender = Gender.PRIVATE,
                myCriteria = List(64) { false },
                theirCriteria = List(64) { false }
            )
        )
        whenever(CriteriaManager.encodeCriteria(any<List<Boolean>>())).thenReturn(ByteArray(8))

        // Use reflection to inject mocks (since fields are private)
        viewModel = MainViewModel()
        val bleScannerField = MainViewModel::class.java.getDeclaredField("bleScanner")
        bleScannerField.isAccessible = true
        bleScannerField.set(viewModel, bleScanner)
        val bleAdvertiserField = MainViewModel::class.java.getDeclaredField("bleAdvertiser")
        bleAdvertiserField.isAccessible = true
        bleAdvertiserField.set(viewModel, bleAdvertiser)
        val locationHelperField = MainViewModel::class.java.getDeclaredField("locationHelper")
        locationHelperField.isAccessible = true
        locationHelperField.set(viewModel, locationHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start initializes BLE components and starts scanning`() = runTest {
        viewModel.start(context)
        verify(bleScanner).startScanning()
        verify(bleAdvertiser).startAdvertising(any<ByteArray>(), any(), any())
        verify(locationHelper).fetchCurrentLocation(any(), any())
        assertEquals(true, viewModel.isSweeping.value)
    }

    @Test
    fun `start does nothing if profile is null`() = runTest {
        whenever(CriteriaManager.getUserProfile(context)).thenReturn(null)
        viewModel.start(context)
        verify(bleScanner, times(0)).startScanning()
        verify(bleAdvertiser, times(0)).startAdvertising(any<ByteArray>(), any(), any())
        assertEquals(false, viewModel.isSweeping.value)
    }

    @Test
    fun `onMatchFound adds new match to matchResults`() = runTest {
        val match = MatchResult(
            id = "otherUserId",
            matchPercentage = 75,
            distanceRssi = -50,
            gender = Gender.MALE
        )

        // Simulate BLEScanner callback
        viewModel.start(context)
        val onMatchFound: (MatchResult) -> Unit = { newMatch ->
            val currentMatches = viewModel.matchResults.value
            if (currentMatches.none { it.id == newMatch.id }) {
                viewModel.matchResults.value = currentMatches + newMatch
            }
        }
        onMatchFound(match)

        assertEquals(listOf(match), viewModel.matchResults.value)
    }

    @Test
    fun `onMatchFound does not add duplicate match`() = runTest {
        val match = MatchResult(
            id = "otherUserId",
            matchPercentage = 75,
            distanceRssi = -50,
            gender = Gender.MALE
        )

        // Simulate BLEScanner callback
        viewModel.start(context)
        val onMatchFound: (MatchResult) -> Unit = { newMatch ->
            val currentMatches = viewModel.matchResults.value
            if (currentMatches.none { it.id == newMatch.id }) {
                viewModel.matchResults.value = currentMatches + newMatch
            }
        }
        onMatchFound(match)
        onMatchFound(match)

        assertEquals(listOf(match), viewModel.matchResults.value)
    }

    @Test
    fun `stop stops BLE components and updates state`() = runTest {
        viewModel.start(context)
        viewModel.stop()
        verify(bleScanner).stopScanning()
        verify(bleAdvertiser).stopAdvertising()
        assertEquals(false, viewModel.isSweeping.value)
    }

    @Test
    fun `requestPermissions launches permission request`() = runTest {
        val permissions = listOf("android.permission.BLUETOOTH_SCAN")
        // Initially, permissionError is null, so no launch
        viewModel.requestPermissions(permissionLauncher)
        verify(permissionLauncher, times(0)).launch(any())

        // Simulate permission error
        viewModel.start(context)
        val onPermissionMissing: (List<String>) -> Unit = { perms ->
            viewModel.permissionError.value = perms
        }
        onPermissionMissing(permissions)

        viewModel.requestPermissions(permissionLauncher)
        verify(permissionLauncher).launch(permissions.toTypedArray())
    }
}