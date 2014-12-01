package nl.wiegman.weatherstation.service.alarm.impl;

import nl.wiegman.weatherstation.R;
import nl.wiegman.weatherstation.SensorType;
import nl.wiegman.weatherstation.sensorvaluelistener.SensorValueListener;
import nl.wiegman.weatherstation.service.alarm.SensorValueAlarmService;
import nl.wiegman.weatherstation.service.data.SensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.AbstractSensorDataProviderService;
import nl.wiegman.weatherstation.service.data.impl.PreferredSensorDataProviderService;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SensorValueAlarmServiceImpl extends Service implements SensorValueAlarmService, SensorValueListener {
	private final String LOG_TAG = this.getClass().getSimpleName();
	
	private static final int ALARM_NOTIFICATION_ID = 1;
	private final String ALARM_NOTIFICATION_TAG = this.getClass().getSimpleName();
	
	private NotificationCompat.Builder notificationBuilder;
	
	private SensorDataProviderService sensorDataProviderService;
	
	private AlarmStrategy[] strategies;
	
	@Override
	public void onCreate() {
		notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
		super.onCreate();
	}
	
	@Override
	public void activate(AlarmStrategy ... strategies) {
		this.strategies = strategies;

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
	public void valueUpdate(Context context, SensorType sensorType,	Double updatedValue) {
		for (AlarmStrategy strategy : strategies) {
			if (strategy.getSensorType() == sensorType) {
				Double alarmValue = getAlarmValue(strategy);
				if (alarm(updatedValue, alarmValue, strategy)) {
					createNotification(strategy, updatedValue, alarmValue);
				}
			}			
		}
	}
	
	private boolean alarm(Double updatedValue, Double alarmValue, AlarmStrategy strategy) {
		boolean alarmEnabled = isAlarmEnabled(strategy);
		return alarmEnabled && strategy.isAlarmConditionMet(updatedValue, alarmValue);
	}
	
	private Double getAlarmValue(AlarmStrategy strategy) {
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
    	return (double) sharedPreferences.getFloat(strategy.getValueAlarmValuePreferenceKey(getApplicationContext()), 0.0f);
	}

	private boolean isAlarmEnabled(AlarmStrategy strategy) {
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
    	return sharedPreferences.getBoolean(strategy.getValueAlarmEnabledPreferenceKey(getApplicationContext()), false);
	}

	private void registerAsSensorValueListener() {
		for (AlarmStrategy strategy : strategies) {
			sensorDataProviderService.addSensorValueListener(this, strategy.getSensorType());
		}
	}
	
	private void unregisterAsSensorValueListener() {
		for (AlarmStrategy strategy : strategies) {
			sensorDataProviderService.removeSensorValueListener(this, strategy.getSensorType());
		}
	}
	
	private void createNotification(AlarmStrategy strategy, Double updatedValueInStorageUnit, Double alarmValueInStorageUnit) {
		String message = strategy.getAlarmNotificationText(getApplicationContext(), updatedValueInStorageUnit, alarmValueInStorageUnit);
		
		Log.i(this.getClass().getSimpleName(), "Notification message: " + message);
		
		configureNotificationBuilder(strategy.getValueAlarmNotificationTitle(getApplication()), message);
		
		// Needed for autoCancel to work...
		PendingIntent notifyPIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);     
		notificationBuilder.setContentIntent(notifyPIntent);
		
		NotificationManager notificationyMgr = (NotificationManager) getApplicationContext().getSystemService(Activity.NOTIFICATION_SERVICE);
		notificationyMgr.notify(ALARM_NOTIFICATION_TAG, ALARM_NOTIFICATION_ID, notificationBuilder.build());
	}

	private void configureNotificationBuilder(String title, String message) {
		notificationBuilder
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(title)
			.setContentText(message)
			.setOnlyAlertOnce(true)
			.setAutoCancel(true)
			.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
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
	
	private final IBinder binder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

    public class LocalBinder extends Binder {
        public SensorValueAlarmServiceImpl getService() {
            return SensorValueAlarmServiceImpl.this;
        }
    }

}
