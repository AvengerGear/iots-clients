package io.iots.android;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class IOTS {
	private MqttClient client = null;
	private String endpointId = null;
	private String collectionId = null;
	private String endpointPassphrase = null;
	private final String PublicSystemTopic = "00000000-0000-0000-0000-000000000000";
	private String PrivateSystemTopic = null;
	private String CollectionTopic = null;
	private String EndpointTopic = null;
	MqttConnectOptions connectOptions = new MqttConnectOptions();
	
	private Random random = new Random();
	private IOTSMessageComposer composer = new IOTSMessageComposer();
    MemoryPersistence persistence = new MemoryPersistence();
	
	private enum SystemCallAPI {
		TOPIC("$API/Topic"), ENDPOINT("$API/Endpoint");
		
		private final String topic;
		SystemCallAPI (final String topic) {
			this.topic = topic;
		}
		
		public String getTopic(){
			return this.topic;
		}
	};

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

	private class IOTSInternalCallback implements MqttCallback {
		private HashMap<String, IOTSMessageCallback> messageCallbacks = new HashMap<String, IOTSMessageCallback>();
		
		@Override
		public void connectionLost(Throwable cause) {
			// IOTS.this.connectionLost();
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			// TODO
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			JSONObject parsedMessage = (JSONObject) new JSONTokener (new String(message.getPayload(), "UTF-8")).nextValue();
			String id, source;
			Object content;
			ContentType type = null;
			IOTSMessageCallback callback = null;

			try {
				id = parsedMessage.getString("id");
			} catch (JSONException e) {
				id = null;
			}
			
			try {
				source = parsedMessage.getString("source");
			} catch (JSONException e) {
				source = null;
			}
			
			try {
				content = parsedMessage.getString("content");
			} catch (JSONException e) {
				content = null;
			}

			try {
				type = ContentType.parseType(parsedMessage.getInt("type"));
			} catch (JSONException e) {
				type = null;
			}
			
			if (type == ContentType.JSON && content instanceof String) {
				content = (JSONObject) new JSONTokener ((String) content).nextValue();
			}
			
			if (id != null && (callback = messageCallbacks.get(id)) != null) {
				callback.onMessage(topic, id, source, type, content);
			}
			
			// TODO: Topic Routing
			// TODO: Default Routing
		}
		
		public void addMessageCallback(String threadId, IOTSMessageCallback callback) {
			messageCallbacks.put(threadId, callback);
		}
		
		public void removeMessageCallback(String id) {
			messageCallbacks.remove(id);
		}

	}
	
	private IOTSInternalCallback callback = null;

	
	/* CONSTRUCTORS */

	private void init(String endpointId, String passphrase, String uri) throws MqttException{
		this.client = new MqttClient(uri, endpointId, persistence);
		this.callback = new IOTSInternalCallback();
		this.endpointId = endpointId;
		this.endpointPassphrase = passphrase;
		this.PrivateSystemTopic = "private/" + this.endpointId; // TODO: change this to random id.
		
		this.connectOptions = new MqttConnectOptions();
		this.connectOptions.setCleanSession(true);
		this.connectOptions.setUserName(this.endpointId);
		this.connectOptions.setPassword(this.endpointPassphrase.toCharArray());
	}
	
	public IOTS(String endpointId, String passphrase, boolean ssl) throws MqttException{
		String uri = null;
		if (ssl) {
			uri = "ssl://iots.io:1883";
		} else {
			uri = "tcp://iots.io:1883";
		}
		init(endpointId, passphrase, uri);
	}
	
	public IOTS(String endpointId, String passphrase, String uri) throws MqttException{
		init(endpointId, passphrase, uri);
	}

	public IOTS(String endpointId, String passphrase) throws MqttException{
		this(endpointId, passphrase, true);
	}

	public IOTS(String endpointId, boolean ssl) throws MqttException{
		this(endpointId, null, true);
	}
	
	public IOTS(String endpointId) throws MqttException{
		this(endpointId, true);
	}
	
	public String createMessageThread(IOTSMessageCallback callback){
		String threadId = System.currentTimeMillis() + new BigInteger(128, random).toString(16);
		this.callback.addMessageCallback(threadId, callback);
		return threadId;
	}
	
	public void removeMessageThread(String threadId){
		this.callback.removeMessageCallback(threadId);
	}
	
	public void connect() throws MqttException{
		this.client.connect(this.connectOptions);
		this.client.setCallback(this.callback);
		this.client.subscribe(this.PublicSystemTopic, 0);
		this.client.subscribe(this.PrivateSystemTopic, 0);
		
		JSONObject authParams = null;
		try {
			authParams = new JSONObject().put("cmd", "Auth");
			if (this.endpointPassphrase != null) {
				authParams.put("passphrase", this.endpointPassphrase);
			}

			this.systemCall(SystemCallAPI.ENDPOINT, authParams, new IOTSMessageCallback() {
				@Override
				public void onMessage(String topic, String id, String source, ContentType type, Object content) {
					// Assume that the content is JSONObject.
					try {
						JSONObject parsedMessage = (JSONObject) content;
						IOTS.this.collectionId = parsedMessage.getString("_collection");
						IOTS.this.CollectionTopic = IOTS.this.collectionId;
						IOTS.this.EndpointTopic = IOTS.this.collectionId + "/" + IOTS.this.endpointId;
						synchronized(IOTS.this) {
							IOTS.this.notify(); // Notify the main IOTS thread.
						}
						IOTS.this.subscribe(IOTS.this.CollectionTopic);
						IOTS.this.subscribe(IOTS.this.EndpointTopic);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			synchronized(this) {
				this.wait(10000); // Wait for reply.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void disconnect() throws org.eclipse.paho.client.mqttv3.MqttException{
		client.disconnect();
	}
	
	public void createTopic(String topic) {
		// TODO
	}
	
	public void subscribe(String topic) throws MqttException {
		this.client.subscribe(topic, 0);
	}
	
	public void unsubscribe(String topic) throws MqttException {
		this.client.unsubscribe(topic);
	}
	
	public void publish(String topic, Object content) {
		String messageId = System.currentTimeMillis() + new BigInteger(128, random).toString(16);
		this.publish(topic, content, messageId);
	}
	
	public void publish(String topic, Object content, String messageId) {
		MqttMessage message;
		try {
			String payload = composer.compose(messageId, this.PrivateSystemTopic, content);
			message = new MqttMessage(payload.getBytes());
			this.client.publish(topic, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void request(String topic, Object content, final IOTSMessageCallback callback) {
		String threadId = this.createMessageThread(new IOTSMessageCallback(){
			@Override
			public void onMessage(String topic, String threadId, String source,
					ContentType type, Object content) {
				callback.onMessage(topic, threadId, source, type, content);
				IOTS.this.removeMessageThread(threadId);
			}
		});
		this.publish(topic, content, threadId);
	}
	
	private void systemCall(SystemCallAPI api, Object content, IOTSMessageCallback callback){
		this.request(api.getTopic(), content, callback);
	}

	public String getEndpointId() {
		return endpointId;
	}

	public String getCollectionId() {
		return collectionId;
	}
}