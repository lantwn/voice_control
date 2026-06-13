package com.voicerider.navigation.planner

import com.voicerider.core.model.RouteInfo
import com.voicerider.core.util.Logger

class RoutePlanner {
    private var amapSdkAvailable = false

    fun initialize(webApiKey: String): Boolean {
        Logger.i("RoutePlanner: initializing with AMap key")
        // TODO: Replace with AMap Services SDK init
        // AMapLocationClient.setApiKey(webApiKey)
        amapSdkAvailable = true
        return true
    }

    suspend fun planRoute(
        fromName: String,
        toName: String,
        fromLat: Double = 0.0,
        fromLng: Double = 0.0,
        toLat: Double = 0.0,
        toLng: Double = 0.0,
        mode: String = "BIKE"
    ): RouteInfo? {
        if (!amapSdkAvailable) {
            Logger.w("RoutePlanner: AMap SDK not available")
            return null
        }

        Logger.i("RoutePlanner: planning $mode route from '$fromName' to '$toName'")

        // TODO: Replace with AMap route search
        // val query = RouteSearch.Query(fromLatLng, toLatLng, RouteSearch.DrivingDefault)
        // val result = routeSearch.calculateRoute(query)

        return RouteInfo(
            fromName = fromName,
            toName = toName,
            fromLat = fromLat,
            fromLng = fromLng,
            toLat = toLat,
            toLng = toLng,
            distanceMeters = 2300f,
            durationSeconds = 480
        )
    }
}
