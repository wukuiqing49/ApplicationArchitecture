package com.wkq.util.location

import android.location.Location

interface AppLocationProvider {

    val lastLocation: Location?

    fun start(
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    )

    fun stop()

    fun requestSingleUpdate(
        onLocationChanged: (Location) -> Unit,
        onLocationUnavailable: () -> Unit
    )
}
