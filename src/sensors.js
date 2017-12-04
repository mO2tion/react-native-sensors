import { NativeModules, DeviceEventEmitter } from "react-native";
import * as Rx from "rxjs/Rx";
const {
  Gyroscope: GyroNative,
  Accelerometer: AccNative,
  DeviceMotion: MotionNative
} = NativeModules;

const handle = {
  Accelerometer: AccNative,
  Gyroscope: GyroNative,
  DeviceMotion: MotionNative
};

const RNSensors = {
  start: function(type, updateInterval, errorCallback = null) {
    const api = handle[type];
    api.setUpdateInterval(updateInterval);
    api.startUpdates(errorCallback);
  },

  stop: function(type) {
    const api = handle[type];
    api.stopUpdates();
  }
};

function createSensorMonitorCreator(sensorType) {
  function Creator(options = {}) {
    const {
      updateInterval = 100 // time in ms
    } =
      options || {};
    let observer;

    /*
     Only start the sensor once we subscribe to the observable.
     Inspired by https://stackoverflow.com/questions/41883339/observable-onsubscribe-equivalent-in-rxjs
     */
    Rx.Observable.prototype.doOnSubscribe = function(onSubscribe) {
      let source = this;
      return Rx.Observable.defer(() => {
        onSubscribe();
        return source;
      });
    };

    // Instanciate observable
    const observable = Rx.Observable
      .create(function(obs) {
        observer = obs;
        DeviceEventEmitter.addListener(sensorType, function(data) {
          observer.next(data);
        });
      })
      .doOnSubscribe(() => {
        // Start the sensor manager
        RNSensors.start(sensorType, updateInterval, options["errorHandler"]);
      });

    // Stop the sensor manager
    observable.stop = () => {
      RNSensors.stop(sensorType);
      observer.complete();
    };

    return observable;
  }

  return Creator;
}

// TODO: lazily intialize them (maybe via getter)
const Accelerometer = createSensorMonitorCreator("Accelerometer");
const Gyroscope = createSensorMonitorCreator("Gyroscope");
const DeviceMotion = createSensorMonitorCreator("DeviceMotion");

export default {
  Accelerometer,
  Gyroscope,
  DeviceMotion
};
