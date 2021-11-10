package com.mapbox.rctmgl.hypervisor

import android.content.Context
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.rctmgl.R

class ManeuverInstructionsViewManager : SimpleViewManager<MapboxManeuverView>() {
  private lateinit var reactContext: ThemedReactContext
  private lateinit var safeContext: Context

  private lateinit var mapboxNavigation: MapboxNavigation
  private lateinit var maneuverView: MapboxManeuverView

  override fun getName() = "ManeuverInstructionsView"

  override fun createViewInstance(reactContext: ThemedReactContext): MapboxManeuverView {
    this.reactContext = reactContext
    this.safeContext = reactContext.applicationContext
    initMapBox()
    maneuverView = MapboxManeuverView(reactContext)
    maneuverView.updateStyle(R.style.MapboxCustomManeuverStyle)
    return maneuverView
  }

  private fun initMapBox() {
    //TODO remove this delay, see how we can make it wait untill there is a mapbox instance
    Thread.sleep(1000)
    if (MapboxNavigationProvider.isCreated()) {
      mapboxNavigation = MapboxNavigationProvider.retrieve()
      val listener: LifecycleEventListener = object : LifecycleEventListener {
        override fun onHostResume() {
          mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        }

        override fun onHostPause() {
          mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        }

        override fun onHostDestroy() {
          mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        }
      }
      reactContext.addLifecycleEventListener(listener)
    }
  }

  // Define distance formatter options
  private val distanceFormatter: DistanceFormatterOptions by lazy {
    DistanceFormatterOptions.Builder(safeContext).build()
  }
  // Create an instance of the Maneuver API
  private val maneuverApi: MapboxManeuverApi by lazy {
    MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))
  }

  private val routeProgressObserver =
    RouteProgressObserver { routeProgress ->
      val maneuvers = maneuverApi.getManeuvers(routeProgress)
      maneuverView.renderManeuvers(maneuvers)
    }
}
