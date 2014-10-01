package nl.wiegman.weatherstation.history;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDatabase extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "WeatherStationDatabase";
	
    // Contacts table name
    private static final String TABLE_SENSOR_HISTORY = "sensor_history";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SENSOR_NAME = "sensor_name";
    private static final String KEY_SENSOR_VALUE = "sensor_value";
	
    public HistoryDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_SENSOR_HISTORY + "("
                + KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_TIMESTAMP + " DATETIME,"
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
	    values.put(KEY_TIMESTAMP, getDateTime());
	    values.put(KEY_SENSOR_NAME, sensorName);
	    values.put(KEY_SENSOR_VALUE, sensorValue);
	 
	    // Inserting Row
	    db.insert(TABLE_SENSOR_HISTORY, null, values);
	    db.close(); // Closing database connection
	}
	
	private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
}
	
	public List<SensorHistoryItem> getAllHistory() {
	    List<SensorHistoryItem> allHistory = new ArrayList<SensorHistoryItem>();

	    String selectQuery = "SELECT * FROM " + TABLE_SENSOR_HISTORY;
	 
	    SQLiteDatabase db = this.getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	        do {
	        	SensorHistoryItem historyItem = new SensorHistoryItem();
	            historyItem.setId(Integer.parseInt(cursor.getString(0)));
	            historyItem.setTimestamp(Timestamp.valueOf(cursor.getString(1)));
	            historyItem.setSensorName(cursor.getString(2));
	            historyItem.setSensorValue(cursor.getDouble(3));
	            allHistory.add(historyItem);
	        } while (cursor.moveToNext());
	    }
	 
	    // return contact list
	    return allHistory;
	}

	public void deleteAll(String sensorName) {
	    SQLiteDatabase db = this.getWritableDatabase();
	    db.delete(TABLE_SENSOR_HISTORY, "sensor_name = ?", new String[] {sensorName});
	}
}
