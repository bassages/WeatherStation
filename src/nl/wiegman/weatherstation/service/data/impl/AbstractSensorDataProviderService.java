package nl.wiegman.weatherstation.service.data.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public abstract class AbstractSensorDataProviderService extends Service implements SensorDataProviderService {

    private final Map<SensorType, List<SensorValueListener>> sensorValueListeners = new HashMap<SensorType, List<SensorValueListener>>();

	@Override
	public void onCreate() {
		super.onCreate();
		
		for (SensorType sensorType : SensorType.values()) {
			sensorValueListeners.put(sensorType, new ArrayList<SensorValueListener>());
		}
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }
	
    @Override
	public void addSensorValueListener(SensorValueListener sensorValueListener, SensorType sensorType) {
    	this.sensorValueListeners.get(sensorType).add(sensorValueListener);
    }
	
    @Override
	public void removeSensorValueListener(SensorValueListener sensorValueListener, SensorType sensorType) {
    	this.sensorValueListeners.get(sensorType).remove(sensorValueListener);
	}
    
	protected void publishSensorValueUpdate(SensorType sensorType, Double sensorValue) {
		for (SensorValueListener listener : sensorValueListeners.get(sensorType)) {
			listener.valueUpdate(getApplicationContext(), sensorType, sensorValue);
        }
	}
    
	private final IBinder binder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

    public class LocalBinder extends Binder {
        public SensorDataProviderService getService() {
            return AbstractSensorDataProviderService.this;
        }
    }
}
