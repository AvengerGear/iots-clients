package io.iots.android.test;

import java.util.Properties;

import io.iots.android.IOTS;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		try {
			this.setUp();
			this.testConnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("IOTSClientTestApp", "Test completed.");
	}
	private String serverUri;
	private String collectionId;
	private String collectionKey;
	private final String TEST_CONF_FILE = "test.properties";
	
	public void setUp() throws Exception {
		Properties props = new Properties();
		props.load(this.getResources().getAssets().open(TEST_CONF_FILE));
		serverUri = props.getProperty("serverUri");
		collectionId = props.getProperty("collectionId");
		collectionKey = props.getProperty("collectionKey");
	}
	
	public void testConnect() throws Exception {
		final IOTS iots = new IOTS(this.getApplicationContext(), collectionId, collectionKey, serverUri);
		try{
			iots.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		iots.close();
		iots.deleteEndpoint();
	}
}
