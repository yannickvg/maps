//
//  MapboxNavigationModule.m
//  react-native-mapbox-gl
//
//  Created by Dimmy Maenhout on 23/11/2021.
//

#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(MapboxNavigationModule, RCTEventEmitter)

//_RCT_EXTERN_REMAP_METHOD(init, initialize:
//                         (NSString) accessToken
//                         withLanguage:(NSString) language
//                         withEnableLogging:(BOOL)enableLogging
//                         withResolver:(RCTPromiseResolveBlock) resolve
//                         withRejecter: (RCTPromiseRejectBlock) reject, false)

RCT_EXTERN_METHOD(initialize:
                  (NSString) accessToken
                  withLanguage:(NSString) language
                  withEnableLogging:(BOOL)enableLogging
                  withResolver:(RCTPromiseResolveBlock) resolve
                  withRejecter: (RCTPromiseRejectBlock) reject)

RCT_EXTERN_METHOD(updateLanguage:
                  (NSString) language
                  withResolver: (RCTPromiseResolveBlock) resolve
                  withRejecter: (RCTPromiseRejectBlock) reject)

RCT_EXTERN_METHOD(setRoute:
                  (NSString) route
                  withResolver: (RCTPromiseResolveBlock) resolve
                  withRejecter: (RCTPromiseRejectBlock) reject)
@end
