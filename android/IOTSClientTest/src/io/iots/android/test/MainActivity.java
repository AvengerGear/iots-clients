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
			//this.testConnect();
			this.testConnectWithPassphrase();
			//this.testSubscribe();
			//this.testPublish();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("IOTSClientTestApp", "Test completed.");
	}
	
	private String serverUri;
	private String endpointWithoutPassphrase;
	private String endpointWithPassphrase;
	private String collection;
	private String passphrase;
	private final String TEST_CONF_FILE = "test.properties";
	
	public void setUp() throws Exception {
		Properties props = new Properties();
		props.load(this.getResources().getAssets().open(TEST_CONF_FILE));
		serverUri = props.getProperty("serverUri");
		endpointWithoutPassphrase = props.getProperty("endpointWithoutPassphrase");
		endpointWithPassphrase = props.getProperty("endpointWithPassphrase");
		collection = props.getProperty("collection");
		passphrase = props.getProperty("passphrase");
	}
	
	public void testConnect() throws Exception {
		final IOTS iots = new IOTS(endpointWithoutPassphrase, null, serverUri);
		iots.connect();
		if(iots.getCollectionId().equals(MainActivity.this.collection)){
			Log.e("IOTSClientTestApp", "testConnect: success.");
		} else {
			Log.e("IOTSClientTestApp", "testConnect: CollectionId does not match.");
		}
	}

	public void testConnectWithPassphrase() throws Exception {
		final IOTS iots = new IOTS(endpointWithPassphrase, passphrase, serverUri);
		iots.connect();
		if(iots.getCollectionId().equals(MainActivity.this.collection)){
			Log.e("IOTSClientTestApp", "testConnectWithPassphrase: success.");
		} else {
			Log.e("IOTSClientTestApp", "testConnectWithPassphrase: CollectionId does not match.");
		}
	}
	
	public void testSubscribe () throws Exception {
	}
	
	public void testPublish () throws Exception {
	}
	
	public void testCreateTopic() throws Exception {
	}
}
