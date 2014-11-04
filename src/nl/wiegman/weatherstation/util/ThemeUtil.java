package nl.wiegman.weatherstation.util;

import nl.wiegman.weatherstation.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class ThemeUtil {

    public static void setThemeFromPreferences(Activity activity) {
    	String themeFromPreferences = getThemeFromPreferences(activity.getApplicationContext());
    	if ("Dark".equals(themeFromPreferences)) {
    		activity.setTheme(R.style.dark);
    	} else {
    		activity.setTheme(R.style.light);    		
    	}
	}
    
    public static String getThemeFromPreferences(Context context) {
    	String themePreferenceKey = context.getApplicationContext().getString(R.string.preference_theme_key);
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    	return preferences.getString(themePreferenceKey, "Light");
	}
}
