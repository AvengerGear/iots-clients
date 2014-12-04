package io.iots.android;

import org.json.JSONObject;

public enum ContentType {
	PLAIN(0), JSON(1), BINARY(2);
	
	int type;
	
	ContentType (final int type) {
		this.type = type;
	}
	
	public int toInt(){
		return this.type;
	}
	
	static ContentType getType(Object obj) {
		if (obj instanceof JSONObject) {
			return JSON;
		}
		if (obj instanceof String) {
			return PLAIN;
		}
		return BINARY;
	}
	
	static ContentType parseType(int ord) {
		return CONTENT_TYPE_INDEXED[ord];
	}
	
	static final ContentType CONTENT_TYPE_INDEXED[] = {PLAIN, JSON, BINARY};
};

