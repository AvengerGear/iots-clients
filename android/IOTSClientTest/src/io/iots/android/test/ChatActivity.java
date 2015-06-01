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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends ListActivity implements OnKeyListener {
	
	private ArrayAdapter<String> mAdapter;

	private ArrayList<String> mStrings = new ArrayList<String>();
	private TextView mTvUserTest;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);
		setContentView(R.layout.act_chat);
		this.mTvUserTest = (TextView)this.findViewById(R.id.userText);
		mTvUserTest.setOnKeyListener(this);
		
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
		} catch (IOTSException exception){
			Toast.makeText(getApplicationContext(), "Welcome Back", 0).show();
		}
		iots.subscribe(iots.getEndpointTopic() + "/testTopic");
		iots.setDefaultCallback(new IOTSMessageCallback() {
			@Override
			public void onMessage(final String topic, final String threadId, final String source,
					final ContentType type, final Object content,final int status) {
				mHandler.post(new Runnable(){

					@Override
					public void run() {
						Log.w("om","====onMessage====");
						Log.d("om","topic======="+topic);
						Log.d("om","threadId======="+threadId);
						Log.d("om","source======="+source);
						Log.d("om","type======="+type);
						Log.d("om","content======="+content);
						Log.d("om","status======="+status);
						receive("("+topic+")"+source +" says :"+content.toString());						
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
	  public boolean onKey(View v, int keyCode, KeyEvent event) {
	        if (event.getAction() == KeyEvent.ACTION_DOWN) {
	            switch (keyCode) {
	                case KeyEvent.KEYCODE_DPAD_CENTER:
	                case KeyEvent.KEYCODE_ENTER:
	            		TextView tv = (TextView) v; 
	                	push(tv.getText().toString());
	                    return true;
	            }
	        }
	        return false;
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
		
		//iots.deleteEndpoint();
		super.finish();
	}

	private void receive(String msg) {
		mAdapter.add(msg);
//		mUserText.setText(null);
	}
	
	
	private void push(String msg){
//		iots.publish(iots.getEndpointTopic() + "/testTopic","======"+msg+"=====");
		iots.publish(iots.getEndpointTopic() , "======"+msg+"=====");
	}

}
