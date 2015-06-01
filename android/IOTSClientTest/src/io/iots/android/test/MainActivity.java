package io.iots.android.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_main);

	}

	public void goChat(View view){

		Intent intent = new Intent(this,ChatActivity.class);
		this.startActivity(intent);
	}
	
	public void goSwitch(View view){
		Intent intent = new Intent(this,SwitchActivity.class);
		this.startActivity(intent);
	}
	
}
