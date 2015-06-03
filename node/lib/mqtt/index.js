"use strict";

var crypto = require('crypto');
var events = require('events');
var util = require('util');
var mqtt = require('mqtt');
var Negotiator = require('./negotiator');

var MQTTBackend = module.exports = function(endpoint) {
	var self = this;

	self.endpoint = endpoint;
	self.client = null;
	self.receiver = '';
	self.negotiator = new Negotiator(self);
	self.API = {
		Topic: '$API/Topic',
		Endpoint: '$API/Endpoint'
	};
};

util.inherits(MQTTBackend, events.EventEmitter);

MQTTBackend.prototype.register = function(collectionId, accessKey, options, callback) {
	var self = this;

	var opts = {
		host: options.host || 'iots.io',
		port: options.port || 1883,
		anonymous: true
	};

	// Connect to MQTT server with collection ID
	self.connect('collection://' + collectionId, accessKey, opts, function(err) {
		if (err) {
			callback(err);
			return;
		}

		self.client.on('connect', function(packet) {

			var receiver = 'anonymous/' + self.client.options.clientId;

			// Subscribe to private channel to receive messages from server
			self.subscribe(receiver, function() {

				self.client.on('message', function(topic, message, pkg) {
					var data = {
						topic: topic,
						message: message,
						pkg: pkg
					};

					// Handler to deal with command response from server
					self.negotiator.handle(data, function() {
						callback(new Error('Unknown response'));
					});
				});

				// Register a new endpoint
				self.negotiator.request(self.API.Endpoint, receiver, 1, {
					cmd: 'Register',
					collectionId: collectionId,
					accessKey: accessKey,
				}, function(err, message) {

					if (err) {
						callback(err);
						return;
					}

					if (message.status == 403) {
						callback(new Error('Forbidden'));
						return;
					}

					// Success
					if (message.status == 200) {

						// Return endpoint ID and secret key
						callback(null, message.content);

						// Close anonymous connection
						self.client.end();
					}
				});
			});
		});
	});
};

MQTTBackend.prototype.connect = function(username, password, options, callback) {
	var self = this;

	var opts = {
		host: options.host,
		port: options.port || 1883,
		secure: options.secure || false,
		anonymous: options.anonymous || false
	};

	// Using username to be client ID but anonymous with auto-generate ID by server
	var clientId = username || null;
	if (opts.anonymous)
		clientId = null;

	// Create a connection
	self.client = mqtt.createClient(opts.port, opts.host, {
		clientId: clientId,
		username: username || '',
		password: password || ''
	});

	// Do not fire any events from this MQTT backend if it's under anonymous mode
	if (!opts.anonymous) {

		self.receiver = 'private/' + clientId;

		self.client.on('message', function(topic, message, pkg) {
			var data = {
				topic: topic,
				message: message,
				pkg: pkg
			};

			// Handler to deal with command response from server
			self.negotiator.handle(data, function() {
				self.emit('message', topic, message, pkg);
			});
		});

		// TODO: handle error situation
		self.client.on('error', function() {
			console.log(arguments);
		});

		self.client.on('close', function() {
			self.emit('close');
		});

		self.client.on('connect', function(packet) {
			self.emit('connect');
		});
	}

	callback(null);
};

MQTTBackend.prototype.subscribe = function(topicPath, cb) {
	var self = this;

	self.client.subscribe(topicPath, function() {
		if (cb)
			cb();
	});
};

MQTTBackend.prototype.publish = function(topicPath, packet) {
	var self = this;

	if (!packet.id)
		packet.id = Date.now() + crypto.randomBytes(16).toString('hex');

	self.client.publish(topicPath, JSON.stringify(packet));

	return packet.id;
};

MQTTBackend.prototype.request = function(topicPath, type, data, callback) {
	var self = this;

	self.negotiator.request(topicPath, self.receiver, type, data, callback);
};

MQTTBackend.prototype.systemCall = function(APIType, type, data, callback) {
	var self = this;

	self.negotiator.request(self.API[APIType], self.receiver, type, data, callback);
};
