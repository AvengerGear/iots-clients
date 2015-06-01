package io.iots.android.test;

import android.content.Context;

import com.walnutlabs.android.ProgressHUD;

public class Utils {
	
	private static ProgressHUD PROGRESS_HUD;

	public static void showProgressDialog(Context ctx, String msg,boolean cancelable) {
		PROGRESS_HUD = ProgressHUD.show(ctx, msg, true, true, null);
		PROGRESS_HUD.setCancelable(cancelable);
	}
	public static void updateProgressDialog(Context ctx, String msg) {
		if (PROGRESS_HUD==null){
			PROGRESS_HUD = ProgressHUD.show(ctx, msg, true, true, null);
			PROGRESS_HUD.setCancelable(true);
		}
		PROGRESS_HUD.setMessage(msg);
	}

	public static void dismissProgressDialog() {
		if (PROGRESS_HUD != null && PROGRESS_HUD.isShowing())
			PROGRESS_HUD.dismiss();
	}
}
