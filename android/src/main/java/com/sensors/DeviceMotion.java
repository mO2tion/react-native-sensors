package com.sensors;

import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class DeviceMotion extends ReactContextBaseJavaModule implements SensorEventListener {

    private final ReactApplicationContext reactContext;
    private final SensorManager sensorManager;
    private double lastReading = 0.0;
    private int interval;
    private final float[] sampleBuffer;
    private long lastSeenTimestamp;

    private static final int GYRX = 0;
    private static final int GYRY = 1;
    private static final int GYRZ = 2;
    private static final int ACCX = 3;
    private static final int ACCY = 4;
    private static final int ACCZ = 5;

    public DeviceMotion(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.sensorManager = (SensorManager) reactContext.getSystemService(reactContext.SENSOR_SERVICE);
        this.sampleBuffer = new float[6];
    }

    // RN Methods
    @ReactMethod
    public void setUpdateInterval(int newInterval) {
        this.interval = newInterval;
    }

    @ReactMethod
    public void startUpdates(Callback errorCallback) {
        final Sensor accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            // No sensor found, send error message or throw
            if (errorCallback != null) {
                errorCallback.invoke("No Accelerometer found");
            } else {
                throw new RuntimeException("No Accelerometer found");
            }
            return;
        }

        final Sensor gyroscope = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscope == null) {
            // No sensor found, send error message or throw
            if (errorCallback != null) {
                errorCallback.invoke("No Gyroscope found");
            } else {
                throw new RuntimeException("No Gyroscope found");
            }
            return;
        }

        sensorManager.registerListener(this, accelerometer, this.interval * 1000);
        sensorManager.registerListener(this, gyroscope, this.interval * 1000);
    }

    @ReactMethod
    public void stopUpdates() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public String getName() {
        return "DeviceMotion";
    }

    // SensorEventListener Interface
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        try {
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
        } catch (RuntimeException e) {
            Log.e("ERROR",
                    "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        /* Adjust timestamp, save last seen ts and adjust tempMs to be > last seen ts by a little bit.
         * Only record positive values. Sometimes the clock skips back for odd
         * reasons, just ignore it and keep going, the deltas are going to adjust.
         */
        final double tempMs;
        if (sensorEvent.timestamp>=lastSeenTimestamp) {
            tempMs = (double) (sensorEvent.timestamp / 1000000);
            lastSeenTimestamp = sensorEvent.timestamp;
        }
        else {
            tempMs = (lastSeenTimestamp / 1000000) + 1;
            lastSeenTimestamp = (long) (tempMs * 1000000);
        }

        final Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sampleBuffer[ACCX] = sensorEvent.values[0];
            sampleBuffer[ACCY] = sensorEvent.values[1];
            sampleBuffer[ACCZ] = sensorEvent.values[2];
        }
        else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sampleBuffer[GYRX] = sensorEvent.values[0];
            sampleBuffer[GYRY] = sensorEvent.values[1];
            sampleBuffer[GYRZ] = sensorEvent.values[2];
        }

        if (tempMs - lastReading >= interval) {
            lastReading = tempMs;

            final WritableMap map = Arguments.createMap();

            map.putDouble("accx", sampleBuffer[ACCX]);
            map.putDouble("accy", sampleBuffer[ACCY]);
            map.putDouble("accz", sampleBuffer[ACCZ]);
            map.putDouble("gyrx", sampleBuffer[GYRX]);
            map.putDouble("gyry", sampleBuffer[GYRY]);
            map.putDouble("gyrz", sampleBuffer[GYRZ]);
            map.putDouble("timestamp", tempMs);

            sendEvent("DeviceMotion", map);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
