package nl.wiegman.weatherstation.preference;

import nl.wiegman.weatherstation.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.WindowManager;

public class KeepScreenOnHelper {

	public static void setKeepScreenOnFlagBasedOnPreference(Activity activity) {
		if (getKeepScreenOn(activity)) {
			activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

    private static boolean getKeepScreenOn(Activity activity) {
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
    	Boolean defaultValue = Boolean.parseBoolean(activity.getApplicationContext().getString(R.string.preference_keep_screen_on_default_value));
    	String preferenceKeepScreenOnPreferenceKey = activity.getApplicationContext().getString(R.string.preference_keep_screen_on_key);
    	return sharedPreferences.getBoolean(preferenceKeepScreenOnPreferenceKey, defaultValue);
	}
}
