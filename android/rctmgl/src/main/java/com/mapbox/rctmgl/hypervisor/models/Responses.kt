package com.mapbox.rctmgl.hypervisor.models

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress

interface ResponseWrapper

data class BannerInstructionsResponse(val bannerInstructions: BannerInstructions): ResponseWrapper
data class RouteProgressResponse(val routeProgress: RouteProgress): ResponseWrapper
