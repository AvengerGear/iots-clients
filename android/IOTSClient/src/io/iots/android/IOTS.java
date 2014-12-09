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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class IOTS {
	private MqttClient client = null;
	
	private String mqttUri = null;
	
	private String collectionId = null;
	private String collectionKey = null;
	private String endpointId = null;
	private String endpointPassphrase = null;
	
	private final String PublicSystemTopic = "00000000-0000-0000-0000-000000000000";
	private String PrivateSystemTopic = null;
	private String CollectionTopic = null;
	private String EndpointTopic = null;
	
	private Random random = new Random();
	private IOTSMessageComposer composer = new IOTSMessageComposer();
    MemoryPersistence persistence = new MemoryPersistence();
    
	private Context context = null;
	private SQLiteDatabase endpointDatabase = null;
	
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

	private class IOTSInternalCallback implements MqttCallback {
		private HashMap<String, IOTSMessageCallback> idCallbacks = new HashMap<String, IOTSMessageCallback>();
		private HashMap<String, IOTSMessageCallback> topicCallbacks = new HashMap<String, IOTSMessageCallback>();
		IOTSMessageCallback defaultCallback = null;
		
		@Override
		public void connectionLost(Throwable cause) {
			// TODO: IOTS.this.connectionLost();
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			// TODO
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			JSONObject parsedMessage = (JSONObject) new JSONTokener (new String(message.getPayload(), "UTF-8")).nextValue();
			String id, source;
			int status = 0;
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
			
			try {
				status = parsedMessage.getInt("status");
			} catch (JSONException e) {
				status = 0;
			}
			
			if (type == ContentType.JSON && content instanceof String) {
				content = (JSONObject) new JSONTokener ((String) content).nextValue();
			}
			
			boolean hasCallback =
					(id != null && (callback = this.idCallbacks.get(id)) != null) ||
					((callback = this.topicCallbacks.get(topic)) != null) ||
					((callback = this.defaultCallback) != null);
			
			if (hasCallback && callback != null) {
				callback.onMessage(topic, id, source, type, content, status);
			}
		}
		
		public void addIdCallback(String id, IOTSMessageCallback callback) {
			idCallbacks.put(id, callback);
		}
		
		public void removeIdCallback(String id) {
			idCallbacks.remove(id);
		}
		
		public void addTopicCallback(String topic, IOTSMessageCallback callback) {
			topicCallbacks.put(topic, callback);
		}
		
		public void removeTopicCallback(String topic) {
			topicCallbacks.remove(topic);
		}

		public void setDefaultCallback(IOTSMessageCallback callback) {
			this.defaultCallback = callback;
		}
	}
	
	private IOTSInternalCallback callback = null;

	public IOTS(Context context, String collectionId, String collectionKey, String uri) throws MqttException, IOTSException{
		this.mqttUri = uri;
		this.collectionId = collectionId;
		this.collectionKey = collectionKey;
		this.CollectionTopic = this.collectionId;
		this.context = context;
		this.callback = new IOTSInternalCallback();
		this.getEndpoint();
	}
	
	public IOTS(Context context, String collectionId, String collectionKey) throws MqttException, IOTSException{
		this(context, collectionId, collectionKey, "ssl://iots.io:1883");
	}
	
	private void getEndpoint() throws MqttException, IOTSException{
		this.endpointDatabase = new IOTSEndpointDatabaseOpenHelper(context).getWritableDatabase();
		Cursor cursor = this.endpointDatabase.query(
				/* from */ "endpoints", /* select */ new String[]{"endpoint", "passphrase"},
				/* where */ "collection=?", new String[]{this.collectionId},
				null, null, null, /* limit */ "1");
		if (cursor.getCount() == 0){
			this.registerEndpoint();
		} else {
			cursor.moveToFirst();
			this.endpointId = cursor.getString(0);
			this.endpointPassphrase = cursor.getString(1);
		}
		this.endpointDatabase.close();
		this.PrivateSystemTopic = "private/" + this.endpointId;
		this.EndpointTopic = this.collectionId + "/" + IOTS.this.endpointId;
	}

	private void registerEndpoint() throws MqttException, IOTSException{
		Log.v("IOTS", "Registering endpoint.");
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		String clientId = MqttClient.generateClientId(); // Generate a client ID.
		this.client = new MqttClient(this.mqttUri, clientId, persistence);
		connectOptions.setCleanSession(true);
		connectOptions.setUserName("collection://"+this.collectionId);
		connectOptions.setPassword(this.collectionKey.toCharArray());
		this.client.connect(connectOptions);
		this.client.setCallback(this.callback);
		this.PrivateSystemTopic = "private/" + clientId;
		this.client.subscribe(this.PrivateSystemTopic, 0);

		try {
			JSONObject registerParams = new JSONObject().put("cmd", "Register");
			registerParams.put("collectionId", this.collectionId);
			registerParams.put("accessKey", this.collectionKey);

			this.systemCall(SystemCallAPI.ENDPOINT, registerParams, new IOTSMessageCallback() {
				@Override
				public void onMessage(String topic, String id, String source, ContentType type, Object content, int status) {
					// Assume that the content is JSONObject.
					try {
						JSONObject parsedMessage = (JSONObject) content;
						String endpointId = parsedMessage.getString("endpointId");
						String endpointPassphrase = parsedMessage.getString("passphrase");
						IOTS.this.endpointId = endpointId;
						IOTS.this.endpointPassphrase = endpointPassphrase;
						synchronized(IOTS.this) {
							IOTS.this.notifyAll(); // Notify the main IOTS thread.
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			synchronized(this) {
				this.wait(10000); // Wait for reply.
			}

			IOTS.this.client.disconnect();
			IOTS.this.client.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	
		if (endpointId == null) {
			throw new IOTSException("Timed out when registering endpoint.");
		}
		
		Log.v("IOTS", "Endpoint registered: " + endpointId);
		
		ContentValues values = new ContentValues();
		values.put("collection", IOTS.this.collectionId);
		values.put("endpoint", endpointId);
		values.put("passphrase", endpointPassphrase);
		IOTS.this.endpointDatabase.insertOrThrow("endpoints", null, values);

	}
	
	public void connect() throws MqttException{
		this.client = new MqttClient(this.mqttUri, this.endpointId, this.persistence);
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(true);
		connectOptions.setUserName(this.endpointId);
		connectOptions.setPassword(this.endpointPassphrase.toCharArray());
		this.client.connect(connectOptions);
		this.client.setCallback(this.callback);
		this.client.subscribe(this.PublicSystemTopic, 0);
		this.client.subscribe(this.PrivateSystemTopic, 0);
		
		try {
			Log.v("IOTS", "Authenticating endpoint: " + this.endpointId);
			JSONObject authParams = new JSONObject().put("cmd", "Auth");
			if (this.endpointPassphrase != null) {
				authParams.put("passphrase", this.endpointPassphrase);
			}
			
			this.systemCall(SystemCallAPI.ENDPOINT, authParams, new IOTSMessageCallback() {
				@Override
				public void onMessage(String topic, String id, String source, ContentType type, Object content, int status) {
					// Assume that the content is JSONObject.
					try {
						Log.v("IOTS", "Endpoint authenticated");
						JSONObject parsedMessage = (JSONObject) content;
						IOTS.this.collectionId = parsedMessage.getString("_collection");
						synchronized(IOTS.this) {
							IOTS.this.notifyAll(); // Notify the main IOTS thread.
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			synchronized(this) {
				this.wait(10000); // Wait for reply.
			}
			
			this.subscribe(IOTS.this.CollectionTopic);
			this.subscribe(IOTS.this.EndpointTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void disconnect() throws org.eclipse.paho.client.mqttv3.MqttException{
		client.disconnect();
	}

	public IOTSTopic createTopic(String path, boolean restricted, boolean allowSameCollection, boolean hasAccessKey, boolean allowPublish) throws IOTSException {
		if (path == null || path == "") {
			throw new IOTSException("Topic path cannot be empty.");
		}
		
		final IOTSTopic topic = new IOTSTopic();
		
		final int[] resultStatus = {0};
		
		try{
			Log.v("IOTS", "Creating topic: " + path);
			JSONObject params = new JSONObject()
				.put("cmd", "CreateTopic")
				.put("topic", path)
				.put("restriction_mode", restricted)
				.put("restriction_type", allowPublish ? 0 : 1); // restriction_type: 1 -> subscription only

			if (restricted) {
				params.put("group_accessible", allowSameCollection)
					.put("accessKey", hasAccessKey);
			}
			
			this.systemCall(SystemCallAPI.TOPIC, params, new IOTSMessageCallback() {
				@Override
				public void onMessage(String _topic, String id, String source, ContentType type, Object content, int status) {					
					resultStatus[0] = status;

					if (status == 200) {
						Log.v("IOTS", "Topic created");
						topic.readFromJSON((JSONObject) content);
					}

					synchronized(IOTS.this) {
						IOTS.this.notifyAll(); // Notify the main IOTS thread.
					}
				}
			});
			
			synchronized(this) {
				this.wait(10000); // Wait for reply.
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (resultStatus[0] == 400) {
			throw new IOTSException("Topic exists already", 400);
		}
		
		if (resultStatus[0] == 403) {
			throw new IOTSException("No permission to create a topic", 403);
		}
		
		return topic;
	}
	
	public IOTSTopic createTopic(String path) throws IOTSException {
		return this.createTopic(path, true, true, false, true);
	}
	
	public void subscribe(String path, String accessKey) throws MqttException, IOTSException {
		if (path == null || path == "") {
			throw new IOTSException("Topic path cannot be empty.", 400);
		}
		
		final int[] resultStatus = {0};
		
		try{
			Log.v("IOTS", "Subscribing topic: " + path);
			JSONObject params = new JSONObject()
				.put("cmd", "SubscribeRequest")
				.put("topic", path);
			if (accessKey != null) {
				params.put("secretKey", accessKey);
			}

			this.systemCall(SystemCallAPI.TOPIC, params, new IOTSMessageCallback() {
				@Override
				public void onMessage(String _topic, String id, String source, ContentType type, Object content, int status) {
					resultStatus[0] = status;
					synchronized(IOTS.this) {
						IOTS.this.notifyAll(); // Notify the main IOTS thread.
					}
				}
			});
			
			synchronized(this) {
				this.wait(10000); // Wait for reply.
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (resultStatus[0] == 403) {
			throw new IOTSException("The topic is forbidden to subscribe", 403);
		}
		
		if (resultStatus[0] == 404) {
			throw new IOTSException("Topic not found", 404);
		}
		this.client.subscribe(path, 0);
	}
	
	public void subscribe(String topic) throws MqttException, IOTSException {
		this.subscribe(topic, null);
	}
	
	public void unsubscribe(String topic) throws MqttException {
		this.client.unsubscribe(topic);
	}

	private void publish(String topic, Object content, String messageId) {
		MqttMessage message;
		try {
			String payload = composer.compose(messageId, this.PrivateSystemTopic, content);
			Log.v("IOTS Publish", payload);
			message = new MqttMessage(payload.getBytes());
			this.client.publish(topic, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void publish(String topic, Object content) {
		this.publish(topic, content, null);
	}

	private void request(String topic, Object content, final IOTSMessageCallback callback) {
		String threadId = System.currentTimeMillis() + new BigInteger(128, random).toString(16);
		this.callback.addIdCallback(threadId, new IOTSMessageCallback(){
			@Override
			public void onMessage(String topic, String threadId, String source,	ContentType type, Object content, int status) {
				callback.onMessage(topic, threadId, source, type, content, status);
				IOTS.this.callback.removeIdCallback(threadId);
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
	
	public String getEndpointTopic() {
		return this.EndpointTopic;
	}
	
	public String getCollectionTopic() {
		return this.CollectionTopic;
	}
	
	public void addTopicCallback(String topic, IOTSMessageCallback callback) {
		this.callback.addTopicCallback(topic, callback);
	}
	
	public void removeTopicCallback(String topic) {
		this.callback.removeTopicCallback(topic);
	}

	public void setDefaultCallback(IOTSMessageCallback callback) {
		this.callback.setDefaultCallback(callback);
	}
	
	public void close() throws MqttException {
		this.client.close();
	}
	
	public boolean deleteEndpoint() {
		this.endpointDatabase = new IOTSEndpointDatabaseOpenHelper(context).getWritableDatabase();
		int count = this.endpointDatabase.delete("endpoints", "collection=?", new String[]{this.collectionId});
		this.endpointDatabase.close();
		return count > 0;
	}
}
