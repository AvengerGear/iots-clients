package io.iots.android;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class IOTSMessageComposer {
	public String compose(String messageId, String source, Object content) throws JSONException {
		String contentString;
		if (content instanceof JSONObject) {
			contentString = content.toString();
		} else if (content instanceof byte[]) {
			contentString = new String((byte[]) content);
		} else {
			contentString = (String) content;
		}

		JSONStringer object = new JSONStringer();
		
		object.object(); /* begin encoding an object */
		
		if (messageId != null) {
			object.key("id").value(messageId);
		}
		object.key("type").value(ContentType.getType(content).toInt());
		object.key("source").value(source);
		object.key("content").value(contentString);
		object.endObject();
		
		return object.toString();
	}
}
