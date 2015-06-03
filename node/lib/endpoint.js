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
	self.ContentType = {
		Plain: 0,
		JSON: 1,
		Binary: 2
	};
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

Endpoint.prototype.subscribe = function() {
	var self = this;

	var topic = null;
	var options = null;
	var callback = null;
	if (arguments.length == 1) {
		throw new Error('require two parameters at least');
	} else if (arguments.length == 2) {
		topic = arguments[0];
		callback = arguments[1];
	} else {
		topic = arguments[0];
		options = arguments[1]
		callback = arguments[2];
	}

	// No need to check permission and send request if this topic belongs to me
	var pathObj = topic.split('/');
	if (pathObj[0] == self.collectionId && pathObj[1] == self.id) {
		self.backend.subscribe(topic, function() {
			callback(null);
		});
		return;
	}

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
			// Subscribe to topic immediately
			self.backend.subscribe(topic, function() {
				callback(null);
			});
			return;
		}

		callback(null);
	});
};

Endpoint.prototype.publish = function(topic, options, message) {
	var self = this;

	var topic = null;
	var options = null;
	var message = null;
	if (arguments.length == 1) {
		throw new Error('require two parameters at least');
	} else if (arguments.length == 2) {
		topic = arguments[0];
		options = {};
		message = arguments[1];
	} else {
		topic = arguments[0];
		options = arguments[1]
		message = arguments[2];
	}

	return self.backend.publish(topic, {
		type: options.contentType || self.ContentType.Plain,
		source: self.collectionId + '/' + self.id,
		content: message
	});
};

Endpoint.prototype.createTopic = function(topic, options, callback) {
	var self = this;

	// Subscribe request (Content type 1 is JSON)
	self.backend.systemCall('Topic', 1, {
		cmd: 'CreateTopic',
		topic: self.collectionId + '/' + self.id + '/' + topic,
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

		if (message.status == 400) {
			callback(new Error(message.content));
			return;
		}

		callback(null);
	});
};

Endpoint.prototype.queryTopic = function(endpointId, options, callback) {
	var self = this;

	// TODO: Support more options

	// Subscribe request (Content type 1 is JSON)
	self.backend.systemCall('Topic', 1, {
		cmd: 'QueryTopic',
		endpointId: endpointId
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
			// Return topic list
			callback(null, message.content);
		}
	});
};
