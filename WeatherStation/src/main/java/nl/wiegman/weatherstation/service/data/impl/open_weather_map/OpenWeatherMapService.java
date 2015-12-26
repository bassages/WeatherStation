package nl.wiegman.weatherstation.service.data.impl.open_weather_map;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.MainActivity;
import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PeriodicRunnableExecutor;

public class OpenWeatherMapService extends AbstractSensorDataProviderService implements LocationListener {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final String OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric";

    private Double latitude;
    private Double longitude;

    private PeriodicRunnableExecutor locationServicesStateUpdateExecutor;
    private PeriodicRunnableExecutor dataPublisherExecutor;
    private PeriodicRunnableExecutor openWeatherMapDataRetrieverExecutor;

    private Double temperature;
    private Double pressure;
    private Double humidity;

    private LocationManager locationManager;

    @Override
    public void activate() {
        if (openWeatherMapDataRetrieverExecutor == null) {
            openWeatherMapDataRetrieverExecutor = new PeriodicRunnableExecutor("OpenWeatherMapServiceRetriever", new PeriodicDataRetriever())
                    .setPublishRate(TimeUnit.MINUTES.toMillis(15));
        }

        boolean requestUserToEnableLocationServicesWhenDisabled = true;
        updatePosition(requestUserToEnableLocationServicesWhenDisabled);

        if (dataPublisherExecutor == null) {
            dataPublisherExecutor = new PeriodicRunnableExecutor("OpenWeatherMapServicePublisher", new PeriodicDataPublisher()).start();
        }
    }

    private void updatePosition(boolean requestUserToEnableLocationServicesWhenDisabled) {
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        String bestProvider = getBestLocationProvider();
        if (bestProvider == null) {
            if (requestUserToEnableLocationServicesWhenDisabled) {
                requestUserToEnableLocationServices();
            }
        } else {
            stopLocationServicesStateUpdateExecutor();
            startWeatherDataUpdates(bestProvider);
        }
    }

    private void startWeatherDataUpdates(String bestProvider) {
        Location lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
        broadcastMessageAction(R.string.determine_location);

        if (lastKnownLocation != null && lastKnownLocation.getTime() > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
            openWeatherMapDataRetrieverExecutor.start();
        } else {
            broadcastMessageAction(R.string.determine_location);
            locationManager.requestLocationUpdates(bestProvider, 0, 0, this);
        }
    }

    private String getBestLocationProvider() {
        Criteria criteria = new Criteria();
        criteria.setCostAllowed(false);
        criteria.setSpeedRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_LOW);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        boolean enabledOnly = true;
        return locationManager.getBestProvider(criteria, enabledOnly);
    }

    public void onLocationChanged(Location location) {
        if (location != null) {
            locationManager.removeUpdates(this);

            latitude = location.getLatitude();
            longitude = location.getLongitude();

            openWeatherMapDataRetrieverExecutor.start();
        }
    }

    private void requestUserToEnableLocationServices() {
        locationServicesStateUpdateExecutor = new PeriodicRunnableExecutor("PeriodicLocationServicesStateChecker", new PeriodicLocationServicesStateChecker())
                .setPublishRate(TimeUnit.SECONDS.toMillis(1)).start();

        final Intent intent = new Intent(MainActivity.ACTION_REQUEST_LOCATION_SERVICES);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d(LOG_TAG, "onStatusChanged: " + s);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(LOG_TAG, "onProviderEnabled: " + s);
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d(LOG_TAG, "onProviderDisabled: " + s);
    }

    @Override
    public void deactivate() {
        if (dataPublisherExecutor != null) {
            dataPublisherExecutor.stop();
            dataPublisherExecutor = null;
        }
        if (openWeatherMapDataRetrieverExecutor != null) {
            openWeatherMapDataRetrieverExecutor.stop();
            openWeatherMapDataRetrieverExecutor = null;
        }
        stopLocationServicesStateUpdateExecutor();

        temperature = null;
        humidity = null;
        pressure = null;

        latitude = null;
        longitude = null;

        super.deactivate();
    }

    private void stopLocationServicesStateUpdateExecutor() {
        if (locationServicesStateUpdateExecutor != null) {
            locationServicesStateUpdateExecutor.stop();
            locationServicesStateUpdateExecutor = null;
        }
    }

    // TODO: check if network is available
    private JSONObject getCurrentWeatherForPosition() {
        try {
            broadcastMessageAction(R.string.getting_openweathermap_data);

            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, latitude, longitude));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.addRequestProperty("x-api-key", getApplicationContext().getString(R.string.open_weather_map_api_key));

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                json.append(tmp).append("\n");
            }
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // This value will be 404 if the request was not successful
            if (data.getInt("cod") != 200) {
                return null;
            }
            return data;

        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to get weather data from OpenWeatherMap", e);
            return null;
        }
    }

    private class PeriodicDataRetriever implements Runnable {

        @Override
        public void run() {
            if (latitude != null && longitude != null) {
                JSONObject currentWeatherForPosition = getCurrentWeatherForPosition();
                try {
                    if (currentWeatherForPosition != null) {
                        OpenWeatherMapService.this.temperature = currentWeatherForPosition.getJSONObject("main").getDouble("temp");
                        OpenWeatherMapService.this.pressure = currentWeatherForPosition.getJSONObject("main").getDouble("pressure");
                        OpenWeatherMapService.this.humidity = currentWeatherForPosition.getJSONObject("main").getDouble("humidity");

                        String locationName = currentWeatherForPosition.getString("name");
                        broadcastMessageAction(R.string.your_location, locationName);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Unable to get weather data from OpenWeatherMap", e);
                }
            }
        }
    }

    private class PeriodicDataPublisher implements Runnable {

        @Override
        public void run() {
            publishSensorValueUpdate(SensorType.AmbientTemperature, temperature);
            publishSensorValueUpdate(SensorType.Humidity, humidity);
            publishSensorValueUpdate(SensorType.AirPressure, pressure);
        }
    }

    private class PeriodicLocationServicesStateChecker implements Runnable {

        @Override
        public void run() {
            boolean requestUserToEnableLocationServicesWhenDisabled = false;
            updatePosition(requestUserToEnableLocationServicesWhenDisabled);
        }
    }
}
