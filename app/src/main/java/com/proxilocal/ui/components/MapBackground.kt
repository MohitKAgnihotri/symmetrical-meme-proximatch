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
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .zoom(1.0)
                        .pitch(0.0)
                        .build()
                )
                gestures.updateSettings {
                    this.rotateEnabled = false
                    this.pinchToZoomEnabled = false
                    this.scrollEnabled = false
                    this.pitchEnabled = false
                }
                mapboxMap.loadStyle(Style.DARK)
            }
        },
        update = { mapView ->
            if (userLocation != null) {
                val userPosition = com.mapbox.geojson.Point.fromLngLat(
                    userLocation.longitude,
                    userLocation.latitude
                )
                mapView.camera.flyTo(
                    CameraOptions.Builder()
                        .center(userPosition)
                        .zoom(14.0)
                        .pitch(50.0)
                        .build(),
                    MapAnimationOptions.Builder().duration(4000L).build()
                )
            }
        },
        modifier = modifier
    )
}