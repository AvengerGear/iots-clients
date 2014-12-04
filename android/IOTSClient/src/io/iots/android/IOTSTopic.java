package io.iots.android;

import org.json.JSONException;
import org.json.JSONObject;

public class IOTSTopic {
	public String path;
	public boolean restricted;
	public boolean allowSameCollection;
	public String accessKey;
	public boolean allowPublish;
	
	public void readFromJSON(JSONObject obj) {
		try {
			this.path = obj.getString("path");
		} catch (JSONException e) {
			this.path = null;
		}
		try {
			this.accessKey = obj.getString("accessKey");
		} catch (JSONException e) {
			this.accessKey = null;
		}
		try {
			this.allowPublish = obj.getInt("restriction_type") == 0;
		} catch (JSONException e) {
			this.allowPublish = true;
		}
		try {
			this.allowSameCollection = obj.getBoolean("group_accessible");
		} catch (JSONException e) {
			this.allowSameCollection = true;
		}
		try {
			this.restricted = obj.getBoolean("restriction_mode");
		} catch (JSONException e) {
			this.restricted = true;
		}
	}
}
