package com.example.hyperlocal

import android.app.Application
import com.mapbox.common.MapboxOptions

class HyperlocalApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set your access token here
        MapboxOptions.accessToken = "pk.eyJ1IjoibW9oaXRhZ25paG90cmkiLCJhIjoiY21kbXRqdGtkMW0zbTJrczU4bnd1dmN0OSJ9.l4mSJgY0P5CUh0bqq9MaVg"
    }
}