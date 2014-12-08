package io.iots.android.test;

import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttException;

import io.iots.android.ContentType;
import io.iots.android.IOTS;
import io.iots.android.IOTSMessageCallback;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

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
	private IOTS iots;
	
	public void setUp() throws Exception {
		Properties props = new Properties();
		props.load(this.getResources().getAssets().open(TEST_CONF_FILE));
		serverUri = props.getProperty("serverUri");
		collectionId = props.getProperty("collectionId");
		collectionKey = props.getProperty("collectionKey");
	}
	
	public void testConnect() throws Exception {
		iots = new IOTS(this.getApplicationContext(), collectionId, collectionKey, serverUri);
		iots.connect();
		iots.createTopic(iots.getEndpointTopic() + "/testTopic");
		iots.subscribe(iots.getEndpointTopic() + "/testTopic");
		iots.addTopicCallback(iots.getEndpointTopic() + "/testTopic", new IOTSMessageCallback(){
			@Override
			public void onMessage(String topic, String threadId,
					String source, ContentType type, Object content,
					int status) {
				Log.d("IOTSTest", "Message Received:" + content.toString());
			}
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void finish() {
		try {
			iots.close();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iots.deleteEndpoint();
		super.finish();
	}
}
