package io.iots.android.test;

import java.util.ArrayList;

import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttException;
import io.iots.android.ContentType;
import io.iots.android.IOTS;
import io.iots.android.IOTSException;
import io.iots.android.IOTSMessageCallback;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	
	private String mInComingMessage = ""; 
	private ArrayAdapter<String> mAdapter;

	private ArrayList<String> mStrings = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);
		setContentView(R.layout.list_12);

		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mStrings);

		setListAdapter(mAdapter);
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
		iots = new IOTS(this.getApplicationContext(), collectionId,
				collectionKey, serverUri);
		iots.connect();
		try {
			iots.createTopic(iots.getEndpointTopic() + "/testTopic");
			// TODO : check if the topic with the sane name is already created
		} catch (IOTSException exception){
			// TODO : specify a clear error message , or this should be handled by the client side?
			Toast.makeText(getApplicationContext(), "Welcome Back", 0).show();
		}
		iots.subscribe(iots.getEndpointTopic() + "/testTopic");
		// iots.addTopicCallback(iots.getEndpointTopic() + "/testTopic", new
		// IOTSMessageCallback(){
		// @Override
		// public void onMessage(String topic, String threadId,
		// String source, ContentType type, Object content,
		// int status) {
		// Log.d("IOTSTest", "Message Received:" + content.toString());
		// }
		// });

		iots.setDefaultCallback(new IOTSMessageCallback() {
			@Override
			public void onMessage(String topic, String threadId, String source,
					ContentType type, Object content, int status) {
				mInComingMessage = content.toString();
				mHandler.post(new Runnable(){

					@Override
					public void run() {
						sendText();						
					}});
				Log.d("IOTSTest", "Message Received from " + topic + ":"
						+ content.toString());
			}
		});
	}

	Handler mHandler = new Handler();
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
			iots.disconnect();
			iots.close();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		iots.deleteEndpoint();
		super.finish();
	}

	private void sendText() {
		mAdapter.add(mInComingMessage);
//		mUserText.setText(null);
	}
}
