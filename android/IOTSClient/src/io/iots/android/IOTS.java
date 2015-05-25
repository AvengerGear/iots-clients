package io.iots.android;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
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
	private MqttAsyncClient client = null;
	
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

	/**
	 * IOTS Class Constructor. This will register an endpoint if there's no known endpoint in the database.
	 * @param context  The application or activity context from Android
	 * @param collectionId  The collection ID from IOTS server
	 * @param collectionKey  The collection key from IOTS server
	 * @param uri  The service URI you connect to
	 * @throws MqttException
	 * @throws IOTSException
	 * @see #IOTS(Context, String, String)
	 */
	public IOTS(Context context, String collectionId, String collectionKey, String uri) throws MqttException, IOTSException{
		this.mqttUri = uri;
		this.collectionId = collectionId;
		this.collectionKey = collectionKey;
		this.CollectionTopic = this.collectionId;
		this.context = context;
		this.callback = new IOTSInternalCallback();
		this.getEndpoint();
	}

	/**
	 * IOTS Class Constructor. This will register an endpoint if there's no known endpoint in the database.
	 * This will also connect to iots.io via SSL by default.
	 * @param context  The application or activity context from Android
	 * @param collectionId  The collection ID from IOTS server
	 * @param collectionKey  The collection key from IOTS server
	 * @throws MqttException
	 * @throws IOTSException
	 * @see #IOTS(Context, String, String, String)
	 */
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
		String clientId = MqttAsyncClient.generateClientId(); // Generate a client ID.
        IMqttToken token;
		this.client = new MqttAsyncClient(this.mqttUri, clientId, persistence);
		connectOptions.setCleanSession(true);
		connectOptions.setUserName("collection://"+this.collectionId);
		connectOptions.setPassword(this.collectionKey.toCharArray());
        token = this.client.connect(connectOptions);
		this.client.setCallback(this.callback);
		this.PrivateSystemTopic = "private/" + clientId;
        token.waitForCompletion();
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
	
	/**
	 * Connect to IOTS server.
	 * @throws MqttException
	 * @see #close()
	 * @see #disconnect()
	 */
	public void connect() throws MqttException{
        IMqttToken token;
		this.client = new MqttAsyncClient(this.mqttUri, this.endpointId, this.persistence);
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(true);
		connectOptions.setUserName(this.endpointId);
		connectOptions.setPassword(this.endpointPassphrase.toCharArray());
		token = this.client.connect(connectOptions);
        token.waitForCompletion();
		this.client.setCallback(this.callback);
		token = this.client.subscribe(this.PublicSystemTopic, 0);
        token.waitForCompletion();
		token = this.client.subscribe(this.PrivateSystemTopic, 0);
        token.waitForCompletion();

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

	/**
	 * Disconnect from IOTS server.
	 * @throws MqttException
	 * @see #close()
	 * @see #disconnect()
	 */
	public void disconnect() throws org.eclipse.paho.client.mqttv3.MqttException{
		client.disconnect();
	}

	/**
	 * Create a topic under the endpoint.
	 * @param path  Topic path; it should be in the format of "[collection ID]/[endpoint ID]/[topic ID]"
	 * @param restricted  Set to true if the topic has restricted access using the parameters below
	 * @param allowSameCollection  Allow the same collection accessing the endpoint
	 * @param hasAccessKey  Create the topic with access key
	 * @param allowPublish  Allow other endpoints to publish in this topic
	 * @return topic object; the key will be included if hasAccessKey has set to true
	 * @throws IOTSException
	 * @see #createTopic(String)
	 */
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
	
	/**
	 * Shorthand for {@link #createTopic(String, boolean, boolean, boolean, boolean)} with default parameters:
	 * <ul>
	 * <li>Restricted to endpoints of same collection
	 * <li>No access key is created
	 * <li>Allow other endpoints to publish
	 * </ul>
	 * @param path  Topic path; it should be in the format of "[collection ID]/[endpoint ID]/[topic ID]"
	 * @return topic object
	 * @throws IOTSException
	 * @see #createTopic(String, boolean, boolean, boolean, boolean)
	 */
	public IOTSTopic createTopic(String path) throws IOTSException {
		return this.createTopic(path, true, true, false, true);
	}
	
	/**
	 * Subscribe to a topic with access key.
	 * @param path  Topic path
	 * @param accessKey  Access key which is generated during the creation of the topic
	 * @throws MqttException
	 * @throws IOTSException
	 * @see #subscribe(String)
	 * @see #unsubscribe(String)
	 */
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

        IMqttToken token;
		token = this.client.subscribe(path, 0);
        token.waitForCompletion();
    }

	/**
	 * Subscribe to a topic.
	 * @param topic - Topic path
	 * @throws MqttException
	 * @throws IOTSException
	 * @see #subscribe(String, String)
	 * @see #unsubscribe(String)
	 */
	public void subscribe(String topic) throws MqttException, IOTSException {
		this.subscribe(topic, null);
	}

	/**
	 * Unsubscribe from a topic.
	 * @param topic - Topic path
	 * @throws MqttException
	 * @throws IOTSException
	 * @see #subscribe(String, String)
	 * @see #subscribe(String)
	 */
	public void unsubscribe(String topic) throws MqttException {
		this.client.unsubscribe(topic);
	}

	/**
	 * Publish a message to a topic.
	 * @param topic  The target topic
	 * @param content  Content; can be in the format of JSONObject, String or byte[]
	 */
	public void publish(String topic, Object content) {
		MqttMessage message;
		try {
			String payload = composer.compose(null, this.EndpointTopic, content);
			Log.v("IOTS Publish", payload);
			message = new MqttMessage(payload.getBytes());
			this.client.publish(topic, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void request(String topic, Object content, final IOTSMessageCallback callback) {
		String threadId = System.currentTimeMillis() + new BigInteger(128, random).toString(16);
		MqttMessage message;

		this.callback.addIdCallback(threadId, new IOTSMessageCallback(){
			@Override
			public void onMessage(String topic, String threadId, String source,	ContentType type, Object content, int status) {
				callback.onMessage(topic, threadId, source, type, content, status);
				IOTS.this.callback.removeIdCallback(threadId);
			}
		});
		
		try {
			String payload = composer.compose(threadId, this.PrivateSystemTopic, content);
			Log.v("IOTS System Request", payload);
			message = new MqttMessage(payload.getBytes());
			this.client.publish(topic, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void systemCall(SystemCallAPI api, Object content, IOTSMessageCallback callback){
		this.request(api.getTopic(), content, callback);
	}

	/**
	 * Get current endpoint ID.
	 * @return Endpoint ID
	 */
	public String getEndpointId() {
		return endpointId;
	}

	/**
	 * Get current collection ID.
	 * @return Collection ID
	 */
	public String getCollectionId() {
		return collectionId;
	}
	
	/**
	 * Get endpoint topic path.
	 * @return Endpoint topic path
	 */
	public String getEndpointTopic() {
		return this.EndpointTopic;
	}
	
	/**
	 * Get collection topic path.
	 * @return Collection topic path
	 */
	public String getCollectionTopic() {
		return this.CollectionTopic;
	}
	
	/**
	 * Add a callback of topic. When message arrived within the topic, it will pass to the callback.
	 * @param topic  The topic path
	 * @param callback  Callback object
	 * @see #removeTopicCallback(String)
	 * @see #setDefaultCallback(IOTSMessageCallback)
	 */
	public void addTopicCallback(String topic, IOTSMessageCallback callback) {
		this.callback.addTopicCallback(topic, callback);
	}
	
	/**
	 * Remove a callback of topic.
	 * @param topic  The topic path
	 * @see #addTopicCallback(String, IOTSMessageCallback)
	 */
	public void removeTopicCallback(String topic) {
		this.callback.removeTopicCallback(topic);
	}

	/**
	 * Set default callback. When message arrived without any topic callback, the default callback would be used.
	 * @param callback  Callback object.
	 * @see #removeDefaultCallback()
	 * @see #addTopicCallback(String, IOTSMessageCallback)
	 */
	public void setDefaultCallback(IOTSMessageCallback callback) {
		this.callback.setDefaultCallback(callback);
	}
	
	/**
	 * Remove default callback.
	 * @see #setDefaultCallback(IOTSMessageCallback)
	 */
	public void removeDefaultCallback() {
		this.callback.setDefaultCallback(null);
	}

	/**
	 * Close the client and release resources.
	 * @throws MqttException
	 * @see #disconnect()
	 */
	public void close() throws MqttException {
		this.client.close();
	}
	
	/**
	 * Delete the endpoint stored in the database.
	 * @return if the endpoint is deleted.
	 * @see #IOTS(Context, String, String)
	 * @see #IOTS(Context, String, String, String)
	 */
	public boolean deleteEndpoint() {
		this.endpointDatabase = new IOTSEndpointDatabaseOpenHelper(context).getWritableDatabase();
		int count = this.endpointDatabase.delete("endpoints", "collection=?", new String[]{this.collectionId});
		this.endpointDatabase.close();
		return count > 0;
	}
}
