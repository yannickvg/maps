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

  return (
     <View style={styles.container}>
      <ManeuverInstructionsView style={styles.box} />
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
    height: 600,
    width: 300,
    backgroundColor: 'tomato'
  },
  map: {
    flex: 1
  }
});

export default ShowManeuvers;
