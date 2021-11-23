//
//  MapboxNavigationModuleConfig.swift
//  react-native-mapbox-gl
//
//  Created by Dimmy Maenhout on 23/11/2021.
//

class MapboxNavigationModuleConfig {
    let accessToken: String
    var locale: String
    var enableLogging: Bool
    
    init(accessToken: String, locale: String, enableLogging: Bool) {
        self.accessToken = accessToken
        self.locale = locale
        self.enableLogging = enableLogging
    }
}
