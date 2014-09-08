"use strict";

var events = require('events');
var util = require('util');
var MQTTBackend = require('./mqtt');

var Endpoint = module.exports = function(id, passphrase) {
	var self = this;

	self.id = id || null;
	self.passphrase = passphrase || null;
	self.collectionId = null;
	self.backendType = 'mqtt';
	self.backend = null;
};

util.inherits(Endpoint, events.EventEmitter);

Endpoint.prototype.register = function(options, callback) {
	var self = this;

	if (options.backendType)
		self.backendType = options.backendType;

	if (self.backendType == 'mqtt') {
		self.backend = new MQTTBackend(self);
	} else {
		callback(new Error('No such backend'));
		return;
	}

	self.backend.register(options.collectionId || '', options.accessKey || '', options, function(err, endpointInfo) {
		if (err) {
			callback(err);
			return;
		}

		self.id = endpointInfo.endpointId;
		self.passphrase = endpointInfo.passphrase;
		self.collectionId = options.collectionId;

		callback(null);
	});
};

Endpoint.prototype.createConnection = function(options, callback) {
	var self = this;

	if (options.backendType)
		self.backendType = options.backendType;

	if (self.backendType == 'mqtt') {
		self.backend = new MQTTBackend(self);
	} else {
		callback(new Error('No such backend'));
		return;
	}

	self.backend.connect(self.id, self.passphrase, options, function(err) {
		if (err) {
			callback(err);
			return;
		}

		// Forward events
		self.backend.on('connect', function() {
			self.emit('connect');
		});

		self.backend.on('close', function() {
			self.emit('close');
		});

		self.backend.on('error', function() {
			self.emit('error');
		});

		self.backend.on('message', function(topic, message) {
			self.emit('message', topic, message);
		});

		callback(null);
	});
};

Endpoint.prototype.subscribe = function(topic, options, callback) {
	var self = this;

	// Subscribe request (Content type 1 is JSON)
	self.backend.systemCall('Topic', 1, {
		cmd: 'SubscribeRequest',
		topic: topic,
		secretKey: options.secretKey || undefined
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
			console.log('Success');

			// Subscribe to topic immediately
			self.backend.subscribe(topic);
		}

		callback(null);
	});
};

Endpoint.prototype.publish = function(topic, options) {
	var self = this;

	self.backend.subscribe(topic);
};
