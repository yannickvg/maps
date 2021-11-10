package com.mapbox.rctmgl.hypervisor

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.google.gson.Gson
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.maneuver.model.*

fun List<Maneuver>.toJsonParam(gson: Gson) : WritableMap {
    val stringManeuvers = Arguments.createArray()
    this.forEach {
        stringManeuvers.pushString(gson.toJson(it.toMapboxManeuver()))
    }
    val params = Arguments.createMap()
    params.putArray("maneuvers", stringManeuvers)
    return params
}

data class MapboxManeuver(val laneGuidance: Lane?, val maneuverPoint: Point?,
                          val primary: PrimaryManeuver?, val secondary: SecondaryManeuver?,
                          val stepDistance: MapboxStepDistance?,
                          val sub: SubManeuver?)

fun Maneuver.toMapboxManeuver(): MapboxManeuver {
    return MapboxManeuver(laneGuidance = this.laneGuidance, maneuverPoint = this.maneuverPoint, primary = this.primary,
        secondary = this.secondary, stepDistance = this.stepDistance.toMapboxStepDistance(), sub = this.sub)
}

data class MapboxStepDistance(val distanceRemaining: Double?, val totalDistance: Double?)

fun StepDistance.toMapboxStepDistance(): MapboxStepDistance {
    return MapboxStepDistance(distanceRemaining = this.distanceRemaining, totalDistance = this.totalDistance)
}