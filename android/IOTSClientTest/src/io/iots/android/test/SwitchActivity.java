package io.iots.android.test;

import java.util.ArrayList;


import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.holocolorpicker.ValueBar;

import io.iots.android.ContentType;
import io.iots.android.IOTS;
import io.iots.android.IOTSException;
import io.iots.android.IOTSMessageCallback;
import android.app.Activity;
import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SwitchActivity extends Activity implements  OnClickListener {
	
	
//	private Switch mSwitch ;
	private Button mBtSend; 
	private ColorPicker mPicker ;
	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_switch);

		mBtSend = (Button) this.findViewById(R.id.bt_send);
		mBtSend.setOnClickListener(this);


		mPicker = (ColorPicker) findViewById(R.id.picker);
		mPicker.setShowOldCenterColor(false);
		mPicker.setOnColorChangedListener(new OnColorChangedListener(){

			@Override
			public void onColorChanged(int color) {
				findViewById(R.id.bg).setBackgroundColor(color);
				
			}});

		
		setUp();
//		mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){
//
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView,
//					boolean isChecked) {
//				try {
//					if (isChecked){
//						changeColor("#00ff00");
//					}else{
//						changeColor("#000000");
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}});
		
	}

	private String serverUri;
	private String collectionId;
	private String collectionKey;
	private final String TEST_CONF_FILE = "test.properties";
	private IOTS iots;

	public void setUp()  {
		Utils.showProgressDialog(this, "Connecting...",true);

		try{
			Properties props = new Properties();
			props.load(SwitchActivity.this.getResources().getAssets().open(TEST_CONF_FILE));
			serverUri = props.getProperty("serverUri");
			collectionId = props.getProperty("collectionId");
			collectionKey = props.getProperty("collectionKey");
			iots = new IOTS(SwitchActivity.this, collectionId,
					collectionKey, serverUri);
			iots.connect();
			iots.subscribe(iots.getEndpointTopic());
			iots.setDefaultCallback(new IOTSMessageCallback() {
				@Override
				public void onMessage(String topic, String threadId, String source,
						ContentType type, Object content, int status) {
					
					Log.w("om","====onMessage====");
					Log.d("om","topic======="+topic);
					Log.d("om","threadId======="+threadId);
					Log.d("om","source======="+source);
					Log.d("om","type======="+type);
					Log.d("om","content======="+content);
					Log.d("om","status======="+status);
					 
					Toast.makeText(SwitchActivity.this, "echo:"+content.toString(), 0).show();
					completed();
					Log.d("IOTSTest", "Message Received from " + topic + ":"
							+ content.toString());
				}
			});
			completed();
		}catch (Exception e){
			Log.e("nevin","====="+e);
		}
		
//		new Thread(new Runnable(){
//
//			@Override
//			public void run() {
//				try{
//					Properties props = new Properties();
//					props.load(SwitchActivity.this.getResources().getAssets().open(TEST_CONF_FILE));
//					serverUri = props.getProperty("serverUri");
//					collectionId = props.getProperty("collectionId");
//					collectionKey = props.getProperty("collectionKey");
//					iots = new IOTS(SwitchActivity.this, collectionId,
//							collectionKey, serverUri);
//					iots.connect();
//					iots.subscribe(iots.getEndpointTopic());
//					iots.setDefaultCallback(new IOTSMessageCallback() {
//						@Override
//						public void onMessage(String topic, String threadId, String source,
//								ContentType type, Object content, int status) {
//							
//							Log.w("om","====onMessage====");
//							Log.d("om","topic======="+topic);
//							Log.d("om","threadId======="+threadId);
//							Log.d("om","source======="+source);
//							Log.d("om","type======="+type);
//							Log.d("om","content======="+content);
//							Log.d("om","status======="+status);
//							 
//							Toast.makeText(SwitchActivity.this, "echo:"+content.toString(), 0).show();
//							completed();
//							Log.d("IOTSTest", "Message Received from " + topic + ":"
//									+ content.toString());
//						}
//					});
//					completed();
//				}catch (Exception e){
//					
//				}
//			}}).start();
	}

	public void changeColor(String colorString){
		Utils.showProgressDialog(this, "Changing color...",true);
		// validate color
		try {
			Color.parseColor(colorString);
		} catch (IllegalArgumentException e) {
			colorString = "#000000";
		}
		final String color = colorString ;
		
		
		// build command and send
		try {
			JSONObject cmd = new JSONObject();
			cmd.put("cmd", "set");
			cmd.put("color", color);
			iots.publish(collectionId+"/"+"3879f470-fdd7-11e4-bad9-0577ec859bfd", cmd.toString() ); 
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
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
			iots.disconnect();
			iots.close();
		} catch (MqttException e) {
			e.printStackTrace();
		}
		
		//iots.deleteEndpoint();
		super.finish();
	}

	private void completed() {
		
		mHandler.post(new Runnable(){

			@Override
			public void run() {
				Utils.dismissProgressDialog();				
			}});
		
	}

	@Override
	public void onClick(View v) {
		if (v==this.mBtSend){
			String strColor = String.format("#%06X", 0xFFFFFF & mPicker.getColor());
			Toast.makeText(this, ""+ strColor, 0).show();
			changeColor(strColor);
		}
		
	}
}
