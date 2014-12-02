package io.iots.android;

public abstract class IOTSMessageCallback {
	public abstract void onMessage(String topic, String threadId, String source, IOTS.ContentType type, Object content);
}