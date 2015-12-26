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
import android.support.v4.content.LocalBroadcastManager;

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
	public void deactivate() {
		broadcastMessageAction(null);
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }
	
    @Override
	public void addSensorValueListener(SensorValueListener sensorValueListener, SensorType ... sensorTypes) {
    	for (SensorType sensorType : sensorTypes) {
    		List<SensorValueListener> listeners = sensorValueListeners.get(sensorType);
			listeners.add(sensorValueListener);
    	}
    }
	
    @Override
	public void removeSensorValueListener(SensorValueListener sensorValueListener, SensorType ... sensorTypes) {
    	for (SensorType sensorType : sensorTypes) {
    		List<SensorValueListener> listeners = sensorValueListeners.get(sensorType);
			listeners.remove(sensorValueListener);
    	}
	}
    
	protected void publishSensorValueUpdate(SensorType sensorType, Double sensorValue) {
		List<SensorValueListener> listeners = sensorValueListeners.get(sensorType);
		for (SensorValueListener listener : listeners) {
			listener.valueUpdate(getApplicationContext(), sensorType, sensorValue);
        }
	}

	protected void broadcastMessageAction(Integer messageId, Object... parameters) {
		final Intent intent = new Intent(SensorDataProviderService.ACTION_MESSAGE);
		intent.putExtra(SensorDataProviderService.MESSAGEID, messageId);
		intent.putExtra(SensorDataProviderService.MESSAGEPARAMETERS, parameters);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
