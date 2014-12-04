package io.iots.android;

public abstract class IOTSMessageCallback {
	public abstract void onMessage(String topic, String threadId, String source, ContentType type, Object content, int status);
}