package nl.wiegman.weatherstation.service.history.impl;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * {@link SQLiteOpenHelper} for maintaining sensor value history
 */
public class SensorValueHistoryDatabase extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "SensorValueHistoryDatabase";
	
    // Contacts table name
    private static final String TABLE_SENSOR_HISTORY = "sensor_value_history";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SENSOR_NAME = "sensor_name";
    private static final String KEY_SENSOR_VALUE = "sensor_value";
	
    private static SensorValueHistoryDatabase mInstance;
    
    private SensorValueHistoryDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
    public static SensorValueHistoryDatabase getInstance(Context ctx) {
        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
          mInstance = new SensorValueHistoryDatabase(ctx.getApplicationContext());
        }
        return mInstance;
      }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_SENSOR_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_TIMESTAMP + " INTEGER,"
				+ KEY_SENSOR_NAME + " TEXT,"
                + KEY_SENSOR_VALUE + " DOUBLE" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_HISTORY);
 
        // Create tables again
        onCreate(db);
	}

	public void addSensorValue(String sensorName, double sensorValue) {
	    SQLiteDatabase db = this.getWritableDatabase();
	 
	    ContentValues values = new ContentValues();
	    values.put(KEY_TIMESTAMP, System.currentTimeMillis());
	    values.put(KEY_SENSOR_NAME, sensorName);
	    values.put(KEY_SENSOR_VALUE, sensorValue);
	 
	    // Inserting Row
	    db.insert(TABLE_SENSOR_HISTORY, null, values);
	    db.close(); // Closing database connection
	}
		
	public List<SensorValueHistoryItem> getAllHistory(String sensorName) {
	    List<SensorValueHistoryItem> allHistory = new ArrayList<SensorValueHistoryItem>();

	    String selectQuery = "SELECT * FROM " + TABLE_SENSOR_HISTORY + " WHERE " + KEY_SENSOR_NAME + " = ?";
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    
	    Cursor cursor = db.rawQuery(selectQuery, new String[] {sensorName});
	    try {
	    	// looping through all rows and adding to list
	    	if (cursor.moveToFirst()) {
	    		do {
	    			SensorValueHistoryItem historyItem = new SensorValueHistoryItem();
	    			historyItem.setId(Integer.parseInt(cursor.getString(0)));
	    			historyItem.setTimestamp(cursor.getLong(1));
	    			historyItem.setSensorName(cursor.getString(2));
	    			historyItem.setSensorValue(cursor.getDouble(3));
	    			allHistory.add(historyItem);
	    		} while (cursor.moveToNext());
	    	}	    	
	    } finally {
	    	cursor.close();	    	
	    }

	    return allHistory;
	}

	public void deleteAll() {
	    SQLiteDatabase db = this.getWritableDatabase();
	    db.delete(TABLE_SENSOR_HISTORY, null, null);
	}
}
