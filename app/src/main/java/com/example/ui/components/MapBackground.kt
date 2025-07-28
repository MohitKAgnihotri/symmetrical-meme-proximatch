package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.gestures

@Composable
fun MapBackground(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            // --- FIX: We initialize the MapView and disable gestures right away ---
            val mapView = MapView(context)
            val gesturesPlugin: GesturesPlugin = mapView.gestures

            gesturesPlugin.scrollEnabled = false
            //gesturesPlugin.zoomEnabled = false
            gesturesPlugin.rotateEnabled = false
            gesturesPlugin.pitchEnabled = false

            mapView // Return the configured MapView
        },
        update = { mapView ->
            // The map style is loaded in the update block to ensure it's applied
            mapView.getMapboxMap().loadStyleUri(Style.DARK)
        },
        modifier = modifier
    )
}