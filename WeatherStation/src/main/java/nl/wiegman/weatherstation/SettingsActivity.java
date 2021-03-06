package nl.wiegman.weatherstation;

import nl.wiegman.weatherstation.util.KeepScreenOnUtil;
import nl.wiegman.weatherstation.util.ThemeUtil;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class SettingsActivity extends Activity {
    
	private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Use instance field for listener
		// It will not be gc'd as long as this instance is kept referenced
    	preferenceListener = new PreferenceListener();	
    	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
				
		ThemeUtil.setThemeFromPreferences(this);
		
		setContentView(R.layout.activity_settings);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(R.id.activity_settings, new PrefsFragment()).commit();
		}
		
		KeepScreenOnUtil.setKeepScreenOnFlagBasedOnPreference(this);
	}
	
    public static class PrefsFragment extends PreferenceFragment {   
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = NavUtils.getParentActivityIntent(this); 
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP); 
            NavUtils.navigateUpTo(this, intent);
        }
        return true;
    }
    
	/**
	 * Handles changes in the preferences
	 */
	private final class PreferenceListener implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			String preferenceThemeKey = getApplicationContext().getString(R.string.preference_theme_key);
			String preferenceKeepScreenOnPreferenceKey = getApplicationContext().getString(R.string.preference_keep_screen_on_key);
			
			if (key.equals(preferenceThemeKey)) {
				recreate();
			} if (key.equals(preferenceKeepScreenOnPreferenceKey)) {
				KeepScreenOnUtil.setKeepScreenOnFlagBasedOnPreference(SettingsActivity.this);
			}
		}
	}
}
