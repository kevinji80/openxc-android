package com.openxc.sinks;

import com.openxc.measurements.Latitude;
import com.openxc.measurements.Longitude;
import com.openxc.measurements.VehicleSpeed;

import com.openxc.remote.RawMeasurement;

import android.content.Context;

import android.location.Location;
import android.location.LocationManager;

import android.util.Log;


/**
 * Propagate vehicle location updates through the Android location interface.
 *
 * If we have at least latitude, longitude and vehicle speed from
 * the vehicle, we send out a mocked location for the
 * LocationManager.GPS_PROVIDER and VEHICLE_LOCATION_PROVIDER
 * providers.
 *
 * Developers can either use the standard Android location framework
 * with mocked locations enabled, or the specific OpenXC
 * Latitude/Longitude measurements.
 */
public class MockedLocationSink extends ContextualVehicleDataSink {
    public final static String TAG = "MockedLocationSink";
    public final static String VEHICLE_LOCATION_PROVIDER = "vehicle";

    private LocationManager mLocationManager;

    public MockedLocationSink(Context context) {
        super(context);
        mLocationManager = (LocationManager) getContext().getSystemService(
                Context.LOCATION_SERVICE);
        setupMockLocations();
    }

    public void receive(String measurementId, RawMeasurement measurement) {
        super.receive(measurementId, measurement);
        if(measurementId.equals(Latitude.getGenericName()) ||
                measurementId.equals(Longitude.getGenericName())) {
            updateLocation();
        }
    }

    private void updateLocation() {
        if(mLocationManager == null ||
                !containsMeasurement(Latitude.getGenericName()) ||
                !containsMeasurement(Longitude.getGenericName()) ||
                !containsMeasurement(VehicleSpeed.getGenericName())) {
            return;
        }

        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(get(Latitude.getGenericName()).getValue().doubleValue());
        location.setLongitude(get(Longitude.getGenericName()).getValue().doubleValue());
        location.setSpeed(get(VehicleSpeed.getGenericName()).getValue().floatValue());
        location.setTime(System.currentTimeMillis());

        try {
            mLocationManager.setTestProviderLocation(
                    LocationManager.GPS_PROVIDER, location);
            location.setProvider(VEHICLE_LOCATION_PROVIDER);
            mLocationManager.setTestProviderLocation(
                    VEHICLE_LOCATION_PROVIDER, location);
        } catch(SecurityException e) {
            Log.w(TAG, "Unable to use mocked locations, " +
                    "insufficient privileges", e);
        }
    }

    private void setupMockLocations() {
        try {
            mLocationManager.addTestProvider(LocationManager.GPS_PROVIDER,
                    false, false, false, false, false, true, false, 0, 5);
            mLocationManager.setTestProviderEnabled(
                    LocationManager.GPS_PROVIDER, true);

            if(mLocationManager.getProvider(
                        VEHICLE_LOCATION_PROVIDER) == null) {
                mLocationManager.addTestProvider(VEHICLE_LOCATION_PROVIDER,
                        false, false, false, false, false, true, false, 0, 5);
            }
            mLocationManager.setTestProviderEnabled(
                    VEHICLE_LOCATION_PROVIDER, true);
        } catch(SecurityException e) {
            Log.w(TAG, "Unable to use mocked locations, " +
                    "insufficient privileges", e);
            mLocationManager = null;
        }
    }
}
