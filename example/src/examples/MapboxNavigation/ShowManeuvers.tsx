import React, {FC, useState, useEffect} from 'react';
import {Alert, NativeModules, requireNativeComponent} from 'react-native';
import MapboxGL from '@react-native-mapbox-gl/maps';

import sheet from '../../styles/sheet';
import {onSortOptions} from '../../utils';
import TabBarPage from '../common/TabBarPage';

import { StyleSheet, View, Text } from 'react-native';

const ManeuverInstructionsView = requireNativeComponent(
  'ManeuverInstructionsView'
);

const ShowManeuvers: FC<any> = props => {
  useEffect(() => {
     MapboxGL.setAccessToken('pk.eyJ1Ijoic2VudGFzIiwiYSI6ImNramg0dmxhazk3eWEzMXFqbHY0cGc3ZWkifQ.B6kmxSEPA6W_lida1p6MWQ');
      NativeModules.MapboxNavigation.init(
           'pk.eyJ1Ijoic2VudGFzIiwiYSI6ImNramg0dmxhazk3eWEzMXFqbHY0cGc3ZWkifQ.B6kmxSEPA6W_lida1p6MWQ',
           'nl_BE',
           true
         );
  }, []);

  const _mapOptions = Object.keys(MapboxGL.StyleURL)
      .map(key => {
        return {
          label: key,
          data: (MapboxGL.StyleURL as any)[key], // bad any, because enums
        };
      })
      .sort(onSortOptions);

    const [styleURL, setStyleURL] = useState({styleURL: _mapOptions[0].data});

    const onMapChange = (index: number, newStyleURL: MapboxGL.StyleURL): void => {
      setStyleURL({styleURL: newStyleURL});
    };

    const onUserMarkerPress = (): void => {
      Alert.alert('You pressed on the user location annotation');
    };

  return (
     <View style={styles.container}>
        <MapboxGL.MapView styleURL={styleURL.styleURL} style={sheet.matchParent}>
            <MapboxGL.Camera followZoomLevel={12} followUserLocation />

            <MapboxGL.UserLocation onPress={onUserMarkerPress} />
          </MapboxGL.MapView>
     </View>
  );
};

const styles = StyleSheet.create({
  box: {
    width: 300,
    height: 600,
  },
  page: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF'
  },
  container: {
    height: 800,
    width: 400,
    backgroundColor: 'tomato'
  },
  map: {
    flex: 1
  }
});

export default ShowManeuvers;
