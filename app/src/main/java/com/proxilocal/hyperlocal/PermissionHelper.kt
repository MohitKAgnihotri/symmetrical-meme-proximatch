package com.proxilocal.hyperlocal

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Centralized permission management with support for both old and new Android versions.
 */
object PermissionHelper {
    private const val REQUEST_CODE = 1001

    @RequiresApi(Build.VERSION_CODES.S)
    private val bluetoothPermissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT
    )

    private val locationPermission = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissions + locationPermission
        } else {
            locationPermission
        }
    }

    fun hasPermissions(activity: Activity): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions(activity: FragmentActivity) {
        val permissionsToRequest = getRequiredPermissions().filter { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                if (results.all { it.value }) {
                    // All permissions granted
                } else {
                    // Some permissions denied
                }
            }.launch(permissionsToRequest)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                getRequiredPermissions(),
                REQUEST_CODE
            )
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onAllGranted: () -> Unit,
        onDenied: (List<String>) -> Unit
    ) {
        if (requestCode == REQUEST_CODE) {
            val deniedPermissions = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            if (deniedPermissions.isEmpty()) {
                onAllGranted()
            } else {
                onDenied(deniedPermissions)
            }
        }
    }
}