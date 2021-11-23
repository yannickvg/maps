//
//  Logger.swift
//  react-native-mapbox-gl
//
//  Created by Dimmy Maenhout on 23/11/2021.
//

import Foundation
import os.log

class Logger {
    private static let LOG_SUB_SYSTEM = "MapboxNavigation"
    private static let LOG_CATEGORY = "logging"
    
    let os_app = OSLog(subsystem: LOG_SUB_SYSTEM, category: LOG_CATEGORY)
    
    public func logMessage(enableLogging: Bool? = false, _ arguments: Any?) {
        guard let enableLogging = enableLogging else { return }
        if enableLogging {
            guard let arguments = arguments as? [String: Any], let message = arguments["message"] as? String else {
                os_log("MapboxNavigation - error - could not parse message", log: os_app, type: .error)
                return
            }
            os_log("MapboxNavigation - message - %{public}@", log: os_app, type: .error, message)
        }
    }
}
