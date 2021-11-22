package com.mapbox.rctmgl.hypervisor

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.rctmgl.components.mapview.RCTMGLMapView

import com.facebook.react.module.annotations.ReactModule
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.rctmgl.R
import com.mapbox.rctmgl.RCTMGLPackage
import com.mapbox.rctmgl.components.mapview.RCTMGLMapViewManager
import com.mapbox.rctmgl.hypervisor.models.BannerInstructionsResponse
import com.mapbox.rctmgl.hypervisor.models.RouteProgressResponse

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.camera.NavigationCamera.NAVIGATION_TRACKING_MODE_GPS


@ReactModule(name = "MapboxNavigation")
class MapboxNavigationModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext)  {

    private val safeContext = reactContext.applicationContext
    private val gson = GsonBuilder().create()

    private var config: MapboxNavigationModuleConfig? = null
    private var route: DirectionsRoute? = null

    private lateinit var mapboxNavigation: MapboxNavigation

    private var navigationMapRoute: NavigationMapRoute? = null

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
        handleRoute(gson.fromJson(route, DirectionsRoute::class.java))
        promise.resolve(true)
    }

    private fun handleRoute(route: DirectionsRoute) {
        this.route = route
        this.route?.let {
            mapboxNavigation.setRoutes(listOf(it))
            val mapViewManager:RCTMGLMapViewManager = RCTMGLPackage.mapViewManager
            if (mapViewManager.firstMapView != null) {
                if (navigationMapRoute == null) {
                    navigationMapRoute = NavigationMapRoute.Builder(mapViewManager.firstMapView, mapViewManager.firstMapView.mapboxMap, currentActivity as LifecycleOwner).withMapboxNavigation(mapboxNavigation)
                        .withStyle(R.style.MapboxCustomNavigationRouteStyle)
                        .withVanishRouteLineEnabled(true)
                        .build()
                }
                mapViewManager.firstMapView.mapboxMap.moveCamera(CameraUpdateFactory.zoomTo(15.0))
                val navigationCamera = NavigationCamera(mapViewManager.firstMapView.mapboxMap)
                navigationCamera.resetCameraPositionWith(NAVIGATION_TRACKING_MODE_GPS)
                navigationMapRoute!!.addRoute(this.route)
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            sendEvent(reactContext, "onRouteProgressChanged", gson.toJson(RouteProgressResponse(routeProgress = routeProgress)))
        }
    }

    private val bannerInstructionsObserver = object: BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            sendEvent(reactContext, "onNewBannerInstructions", gson.toJson(BannerInstructionsResponse(bannerInstructions = bannerInstructions)))
        }
    }

    @SuppressLint("MissingPermission")
    private fun initMapBox() {
        config?.accessToken?.let {
            val navigationOptions = MapboxNavigation
                .defaultNavigationOptionsBuilder(safeContext, it)
                .build()
            mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
            val listener: LifecycleEventListener = object : LifecycleEventListener {
                override fun onHostResume() {
                    logDebug("onResume")
                    if (MapboxNavigationProvider.isCreated()) {
                        MapboxNavigationProvider.retrieve().registerRouteProgressObserver(routeProgressObserver)
                        MapboxNavigationProvider.retrieve().registerBannerInstructionsObserver(bannerInstructionsObserver)
                        mapboxNavigation.startTripSession()
                    }
                }

                override fun onHostPause() {
                    logDebug("onPause")
                    if (MapboxNavigationProvider.isCreated()) {
                        MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(routeProgressObserver)
                        MapboxNavigationProvider.retrieve().unregisterBannerInstructionsObserver(bannerInstructionsObserver)
                        mapboxNavigation.stopTripSession()
                    }
                }

                override fun onHostDestroy() {
                    logDebug("onDestroy")
                    if (MapboxNavigationProvider.isCreated()) {
                        MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(routeProgressObserver)
                        MapboxNavigationProvider.retrieve().unregisterBannerInstructionsObserver(bannerInstructionsObserver)
                        mapboxNavigation.stopTripSession()
                    }
                }
            }
            reactContext.addLifecycleEventListener(listener)

            //TODO this is temporary, remove when we have routes
            if (route == null) {
                mapboxNavigation.requestRoutes(
                    RouteOptions.builder().applyDefaultParams().accessToken(it)
                        .coordinates(listOf(
                            Point.fromLngLat(4.375606188596978, 51.14121566158028),
                            Point.fromLngLat(4.441955998895892, 51.14179615098452))).build(),
                    object : RoutesRequestCallback {
                        override fun onRoutesReady(routes: List<DirectionsRoute>) {
                            handleRoute(routes[0])
                        }

                        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {}
                        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {}
                    }
                )
            }
        }
    }

    override fun onCatalystInstanceDestroy() {
        logInfo("onDestroy - stop location engine")
        super.onCatalystInstanceDestroy()
    }

    private fun sendEvent(reactContext: ReactContext, eventName: String, response: String) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, response)
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