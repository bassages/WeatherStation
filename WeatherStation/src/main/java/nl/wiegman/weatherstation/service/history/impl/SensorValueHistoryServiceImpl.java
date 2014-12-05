package nl.wiegman.weatherstation.service.history.impl;

import java.util.List;

import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PreferredSensorDataProviderService;
import nl.wiegman.weatherstation.service.history.SensorValueHistoryService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SensorValueHistoryServiceImpl extends Service implements SensorValueHistoryService, SensorValueListener {
	private final String LOG_TAG = this.getClass().getSimpleName();
	
	private SensorDataProviderService sensorDataProviderService;

	@Override
	public void onCreate() {
		super.onCreate();
		deleteAll(getApplicationContext());
	}
	
	@Override
	public void activate() {
        Intent intent = new Intent(this, PreferredSensorDataProviderService.class);
        boolean serviceSucessfullyBound = bindService(intent, dataProviderServiceConnection, Context.BIND_AUTO_CREATE);
        if (!serviceSucessfullyBound) {
        	Log.e(LOG_TAG, "Unable to bind dataprovider service");
        }
	}
	
	@Override
	public void deactivate() {
		unregisterAsSensorValueListener();
		unbindService(dataProviderServiceConnection);
	}

	@Override
	public void valueUpdate(Context context, SensorType sensorType, Double updatedValue) {
		registerUpdatedValue(this, updatedValue, sensorType);
	}
	
	@Override
	public void deleteAll(final Context context) {
		AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				SensorValueHistoryDatabase.getInstance(context).deleteAll();
				return null;
			}
		};
		asyncTask.execute();
	}

    @Override
	public List<SensorValueHistoryItem> getAll(Context context, SensorType sensorType) {
		return SensorValueHistoryDatabase.getInstance(context).getAllHistory(sensorType.name());
	}

	private ServiceConnection dataProviderServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			sensorDataProviderService = ((AbstractSensorDataProviderService.LocalBinder) service).getService();
			registerAsSensorValueListener();
		}		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			unregisterAsSensorValueListener();
		}
	};
	
	private void registerUpdatedValue(final Context context, final Double updatedSensorValue, final SensorType sensorType) {
		if (updatedSensorValue != null) {
			// Do not block the UI thread, by using an aSyncTask
			AsyncTask<Void,Void,Void> asyncTask = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					SensorValueHistoryDatabase.getInstance(context).addSensorValue(sensorType, updatedSensorValue);
					return null;
				}
			};
			asyncTask.execute();
		}
	}
	
	private void registerAsSensorValueListener() {
		for (SensorType sensorType : SensorType.values()) {
			sensorDataProviderService.addSensorValueListener(this, sensorType);
		}
	}
	
	private void unregisterAsSensorValueListener() {
		for (SensorType sensorType : SensorType.values()) {
			sensorDataProviderService.removeSensorValueListener(this, sensorType);
		}
	}
	
	private final IBinder binder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

    public class LocalBinder extends Binder {
        public SensorValueHistoryService getService() {
            return SensorValueHistoryServiceImpl.this;
        }
    }
}
