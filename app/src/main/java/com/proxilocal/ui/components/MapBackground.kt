package com.proxilocal.ui.components

import android.location.Location
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures

@Composable
fun MapBackground(
    userLocation: Location?,
    modifier: Modifier = Modifier
) {
    var mapView: MapView? by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(userLocation, mapView) {
        val currentMapView = mapView
        if (userLocation != null && currentMapView != null) {
            val userPosition = com.mapbox.geojson.Point.fromLngLat(
                userLocation.longitude,
                userLocation.latitude
            )
            currentMapView.camera.flyTo(
                CameraOptions.Builder()
                    .center(userPosition)
                    .zoom(14.0)
                    .pitch(50.0)
                    .build(),
                MapAnimationOptions.Builder().duration(4000L).build()
            )
        }
    }

    AndroidView(
        factory = { context ->
            MapView(context).apply {
                getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .zoom(1.0)
                        .pitch(0.0)
                        .build()
                )
                // --- THE FIX: Using the correct property names from the interface ---
                gestures.updateSettings {
                    this.rotateEnabled = false
                    this.pinchToZoomEnabled = false // Correct property for zoom
                    this.scrollEnabled = false
                    this.pitchEnabled = false
                }
                getMapboxMap().loadStyleUri(Style.DARK)
                mapView = this
            }
        },
        modifier = modifier
    )
}