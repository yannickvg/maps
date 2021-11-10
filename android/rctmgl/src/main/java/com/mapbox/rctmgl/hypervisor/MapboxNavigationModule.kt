package com.mapbox.rctmgl.hypervisor

import android.annotation.SuppressLint
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = "MapboxNavigation")
class MapboxNavigationModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext)  {

    private val safeContext = reactContext.applicationContext
    private val gson = GsonBuilder().create()

    private var config: MapboxNavigationModuleConfig? = null
    private var route: DirectionsRoute? = null

    private lateinit var mapboxNavigation: MapboxNavigation

    override fun getName(): String {
        return "MapboxNavigation"
    }

    @ReactMethod
    fun init(accessToken: String, language: String, enableLogging: Boolean) {
        config = MapboxNavigationModuleConfig(accessToken = accessToken, locale = language, enableLogging = enableLogging)
        initMapBox()
    }

    @ReactMethod
    fun updateLanguage(language: String, promise: Promise) {
        if (!hasBeenInitialised(promise)) return
        config?.let {
            config = it.copy(locale = language)
        }
    }

    @ReactMethod
    fun setRoute(route: String, promise: Promise) {
        this.route = gson.fromJson(route, DirectionsRoute::class.java)
        this.route?.let {
            mapboxNavigation.setRoutes(listOf(it))
        }
        promise.resolve(true)
    }

    private val distanceFormatter: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(safeContext).build()
    }

    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))
    }

    private val routeProgressObserver =
        RouteProgressObserver { routeProgress ->
            val maneuvers = maneuverApi.getManeuvers(routeProgress)
            if (maneuvers.isValue) {
                sendEvent(reactContext, "onManeuversUpdated", maneuvers.value?.toJsonParam(gson))
            }
        }

    @SuppressLint("MissingPermission")
    private fun initMapBox() {
        config?.accessToken?.let {
            val navigationOptions = NavigationOptions.Builder(safeContext)
                .accessToken(it)
                .build()
            mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
            val listener: LifecycleEventListener = object : LifecycleEventListener {
                override fun onHostResume() {
                    logDebug("onResume")
                    mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                    mapboxNavigation.startTripSession()
                }

                override fun onHostPause() {
                    logDebug("onPause")
                    mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                    mapboxNavigation.stopTripSession()
                }

                override fun onHostDestroy() {
                    logDebug("onDestroy")
                    mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                    mapboxNavigation.stopTripSession()
                }
            }
            reactContext.addLifecycleEventListener(listener)

            //TODO this is temporary, remove when we have routes
            if (route == null) {
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultNavigationOptions().coordinatesList(listOf(
                        Point.fromLngLat(4.375606188596978, 51.14121566158028), Point.fromLngLat(4.441955998895892, 51.14179615098452))).build(),
                    object : RouterCallback {
                        override fun onRoutesReady(
                            routes: List<DirectionsRoute>,
                            routerOrigin: RouterOrigin
                        ) {
                            mapboxNavigation.setRoutes(routes)
                        }

                        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                        }

                        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                        }
                    }
                )
            }

        }
    }

    override fun onCatalystInstanceDestroy() {
        logInfo("onDestroy - stop location engine")
        super.onCatalystInstanceDestroy()
    }

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun hasBeenInitialised(promise: Promise): Boolean {
        if (this.config == null) {
            logError("MapboxNavigationModule was not initialised, call init() first")
            promise.reject("MapboxNavigationModule was not initialised, call init() first")
            return false
        }
        return true
    }

    private fun logError(message: String) {
        config?.let { config ->
            if (config.enableLogging) {
                Log.e("MapboxNavigationModule", message)
            }
        }
    }

    private fun logDebug(message: String) {
        config?.let { config ->
            if (config.enableLogging) {
                Log.d("MapboxNavigationModule", message)
            }
        }
    }

    private fun logInfo(message: String) {
        config?.let { config ->
            if (config.enableLogging) {
                Log.i("MapboxNavigationModule", message)
            }
        }
    }
}

data class MapboxNavigationModuleConfig(val accessToken: String, val locale: String, val enableLogging: Boolean)