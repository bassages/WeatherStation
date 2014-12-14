package nl.wiegman.weatherstation.service.data.impl.open_weather_map;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PeriodicRunnableExecutor;

public class OpenWeatherMapService extends AbstractSensorDataProviderService {
    private final String LOG_TAG = this.getClass().getSimpleName();

    private static final String OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&units=metric";

    private Double latitude;
    private Double longitude;

    private PeriodicRunnableExecutor dataPublisherExecutor;
    private PeriodicRunnableExecutor dataRetrieverExecutor;

    private Double temperature;
    private Double pressure;
    private Double humidity;

    @Override
    public void activate() {
        updatePosition();

        if (dataPublisherExecutor == null) {
            dataPublisherExecutor = new PeriodicRunnableExecutor("OpenWeatherMapServicePublisher", new PeriodicDataPublisher()).start();
        }
        if (dataRetrieverExecutor == null) {
            dataRetrieverExecutor = new PeriodicRunnableExecutor("OpenWeatherMapServiceRetriever", new PeriodicDataRetriever())
                                            .setPublishRate(TimeUnit.MINUTES.toMillis(15))
                                            .start();
        }
    }

    private void updatePosition() {
        Criteria criteria = new Criteria();
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(criteria, true);
        Location devicelocation = locationManager.getLastKnownLocation(provider);

        latitude = devicelocation.getLatitude();
        longitude = devicelocation.getLongitude();
    }

    @Override
    public void deactivate() {
        if (dataPublisherExecutor != null) {
            dataPublisherExecutor.stop();
            dataPublisherExecutor = null;
        }
        if (dataRetrieverExecutor != null) {
            dataRetrieverExecutor.stop();
            dataRetrieverExecutor = null;
        }
    }

    private JSONObject getCurrentWeatherForPosition() {
        try {
            URL url = new URL(String.format(OPEN_WEATHER_MAP_API, latitude, longitude));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.addRequestProperty("x-api-key", getApplicationContext().getString(R.string.open_weather_map_api_key));

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuffer json = new StringBuffer(1024);
            String tmp = "";
            while ((tmp = reader.readLine()) != null) {
                json.append(tmp).append("\n");
            }
            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // This value will be 404 if the request was not
            // successful
            if (data.getInt("cod") != 200) {
                return null;
            }

            return data;
        } catch (Exception e) {
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
                    }
                } catch (JSONException e) {
                    Log.w("Unable to get weather from Open Weather Map API", e);
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
}
