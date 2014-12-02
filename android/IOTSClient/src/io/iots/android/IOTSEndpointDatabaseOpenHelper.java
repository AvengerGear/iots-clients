package io.iots.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IOTSEndpointDatabaseOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "iots";
    private static final int DATABASE_VERSION = 1;
    private static final String ENDPOINTS_TABLE_CREATE =
                "CREATE TABLE endpoints (collection TEXT, endpoint TEXT, passphrase TEXT);";

    public IOTSEndpointDatabaseOpenHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(ENDPOINTS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// No upgraded needed, yet.
	}

}
