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
		String result = new JSONStringer()
			.object()
				.key("type").value(IOTS.ContentType.getType(content).toInt())
				.key("id").value(messageId)
				.key("source").value(source)
				.key("content").value(contentString)
			.endObject()
			.toString();
		return result;
	}
}
