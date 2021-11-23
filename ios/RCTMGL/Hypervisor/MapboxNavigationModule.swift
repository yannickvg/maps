//
//  MapboxNavigationModule.swift
//  react-native-mapbox-gl
//
//  Created by Dimmy Maenhout on 23/11/2021.
//

import Foundation
import MapboxCoreNavigation
import MapboxNavigation
import MapboxDirections

@objc(MapboxNavigationModule)
class MapboxNavigationModule: RCTEventEmitter {
    
    // MARK: - Properties
    
    private let logger = Logger()
    private var config: MapboxNavigationModuleConfig?
    private let message = "message"
    private let MapboxNavigationConfigModule = "MapboxNavigationModuleConfig has not been initialized, call init()"
    
    private var navigationViewController: NavigationViewController?
    
    private var route: DirectionsResult?//Route?
    
    // MARK: - init
    
    @objc(initialize:withLanguage:withEnableLogging:withResolver:withRejecter:)
    func initialize(_ accessToken: String, language: String, enableLogging: Bool, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        config = MapboxNavigationModuleConfig(accessToken: accessToken, locale: language, enableLogging: enableLogging)
        
        logger.logMessage(enableLogging: config?.enableLogging, [message: "initialize"])
        resolve(true)
    }
    
    // MARK: - Private methods
    
    private func isConfigInitialized(reject: RCTPromiseRejectBlock) -> MapboxNavigationModuleConfig? {
        guard let config = config else {
            logger.logMessage(enableLogging: true, [message: "\(MapboxNavigationConfigModule)"])
            reject(nil, "\(MapboxNavigationConfigModule)", nil)
            return nil
        }
        return config
    }
    
    private func convertObjectToJsonString<T: Encodable>(object: T) -> String? {
        do {
            let jsonData = try JSONEncoder().encode(object)
            return String(data: jsonData, encoding: .utf8)
        } catch {
            return nil
        }
    }
    
    private func convertJsonStringToMapboxRoute<T: Decodable>(jsonString: NSString, reject: RCTPromiseRejectBlock) -> T? {
        do {
            let data: Data = jsonString.data(using: String.Encoding.utf8.rawValue)!
            let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
            print("json: \(String(describing: json))")
            guard let json = json,
                  let tempRouteOptions = json["routeOptions"] as? [String:Any] else { return nil }
            let coordinates = tempRouteOptions["coordinates"] as? [[Double]]
            guard let waypoints: [Waypoint] = coordinates?.map({ coors in
                Waypoint(coordinate: CLLocationCoordinate2D(latitude: coors[0], longitude: coors[1]))
            }) else { return nil }
            let routeOptions = RouteOptions(waypoints: waypoints)
            
            let decoder = JSONDecoder()
            decoder.userInfo[.options] = routeOptions

            let object = try? decoder.decode(T.self, from: data)
            print("object: \(String(describing: object))")
            return object
        } catch {
            print("convertJsonStringToObject, error: \(error)")
            logger.logMessage(enableLogging: config?.enableLogging, [message: "method: convertJsonStringToObject, failed to decode jsonString to object, error: \(error)"])
            reject(nil, "method: convertJsonStringToObject, failed to decode jsonString to object, error: \(error)", nil)
            return nil
        }
    }
    
    private func convertJsonStringToObject<T: Decodable>(jsonString: String, reject: RCTPromiseRejectBlock) -> T? {
        do {
            let jsonData: Data = Data(jsonString.utf8)//jsonString.data(using: .utf8)!
            return try JSONDecoder().decode(T.self, from: jsonData)
        } catch {
            logger.logMessage(enableLogging: config?.enableLogging, [message: "method: convertJsonStringToObject, failed to decode jsonString to object, error: \(error)"])
            reject(nil, "method: convertJsonStringToObject, failed to decode jsonString to object, error: \(error)", nil)
            return nil
        }
    }
    
    // MARK: - Overrides
    
    override func supportedEvents() -> [String]! {
        return [SupportedEvents.onManeuversUpdated.rawValue]
    }
    
    // MARK: - Methods
    
    @objc(updateLanguage:withResolver:withRejecter:)
    func updateLanguage(_ language: String, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        guard let config = isConfigInitialized(reject: reject) else { return }
        config.locale = language
        logger.logMessage(enableLogging: config.enableLogging, [message: "updateLanguage, language: \(language)"])
        resolve(true)
    }
    
    @objc(setRoute:withResolver:withRejecter:)
    func setRoute(route: NSString, resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        guard let config = isConfigInitialized(reject: reject) else { return }
//        let json: NSString = SomeJsonObjectOriginal.json as NSString
        
        let adaptedJson = route
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "name=\"", with: "name=\'")
            .replacingOccurrences(of: "\">", with: "\'>")
            .replacingOccurrences(of: "rate=\"", with: "rate=\'")

        guard let route: Route/*Match*/ /*DirectionsResult*/ = convertJsonStringToMapboxRoute(jsonString: adaptedJson as NSString, reject: reject) else {
            logger.logMessage(enableLogging: config.enableLogging, [message: "setRoute, failed to deserialize string to object (Route)"])
            return
        }
        print("setRoute, route: \(route)")

        self.route = route
        let routeCoors: [[CLLocationCoordinate2D]]? = route.legs.compactMap {
            guard let source = $0.source?.coordinate,
                  let destination = $0.destination?.coordinate else { return nil }
            return [source, destination]
        }
        
        let adaptedRouteCoors = routeCoors.map { nestedArray in
            nestedArray.map { coordinates in
                coordinates.forEach { $0 }
            }
        }
        
        let routeCoordinates: [CLLocationCoordinate2D] = route.legs.compactMap { $0.destination?.coordinate}
        let waypoints: [Waypoint]? = route.legs.compactMap {
            guard let destinationCoordinate = $0.destination?.coordinate else { return nil}
            return Waypoint(coordinate: destinationCoordinate)
        }

        let routeOptions = NavigationRouteOptions(coordinates: routeCoordinates)
        let routeResponse = RouteResponse(httpResponse: nil,
                                          routes: [route],
                                          waypoints: waypoints,
                                          options: .route(routeOptions),
                                          credentials: DirectionsCredentials(accessToken: config.accessToken))
//        let navigationService = MapboxNavigationService(routeResponse: routeResponse, routeIndex: 0, routeOptions: routeOptions)
//        let navigationOptions = NavigationOptions(navigationService: navigationService)
        
//        navigationViewController = NavigationViewController(for: routeResponse, routeIndex: 0, routeOptions: routeOptions, navigationOptions: navigationOptions)
        
        resolve(true)
    }
    
    private func onRouteUpdated() {
        NotificationCenter.default.addObserver(self, selector: #selector(routeProgressUpdated), name: Notification.Name.routeControllerProgressDidChange, object: nil)
    }
    
    @objc private func routeProgressUpdated() {
        let instructions = navigationViewController?.navigationService?.routeProgress.currentLegProgress.currentStepProgress.currentVisualInstruction
        
        let instructionsJsonString = convertObjectToJsonString(object: instructions)
        sendEvent(withName: SupportedEvents.onManeuversUpdated.rawValue, body: instructionsJsonString)
    }
}

//struct MapboxManeuver: Codable {
//    let laneGuidance: String? // TODO change type
//    let maneuverPoint: CLLocationCoordinate2D? // TODO verify type
//    let primary: String? // TODO change type
//    let secondary: String? // TODO change type
//    let stepDistance: Double? // TODO verify type
//    let sub: SubManeuver
//
//}

extension Dictionary {
    mutating func switchKey(fromKey: Key, toKey: Key) {
        if let entry = removeValue(forKey: fromKey) {
            self[toKey] = entry
        }
    }
}

struct LocalCoordinates: Codable {
    let coordinates: [[String]]
}

struct LocalCoordinate: Codable {
    var latitude: Double
    var longitude: Double
}
