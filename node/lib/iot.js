"use strict";

var events = require('events');
var util = require('util');
var Dispatcher = require('./dispatcher');

var IoT = module.exports = function(endpoint) {
	var self = this;

	self.endpoint = endpoint;
	self.dispatcher = new Dispatcher(self);
	self.channels = {};
	self.middlewares = {
		connect: [
			require('./middleware/connect'),
		],
		message: [
			require('./middleware/message')
		]
	};
};

util.inherits(IoT, events.EventEmitter);

IoT.prototype.connect = function() {
	var self = this;

	self.endpoint.auth({}, function(err) {

		if (err) {
			self.emit('error', err);
			return;
		}

		// Setup channels
		self.channels.systemChannel = '00000000-0000-0000-0000-000000000000';
		self.channels.collectionChannel = self.endpoint.collectionId;
		self.channels.endpointChannel = self.endpoint.collectionId + '.' + self.endpoint.id;

		var client = self.endpoint.createConnection();

		client.on('connect', function() {

			self.dispatcher.middleware('connect', client, function() {
				self.emit('connected');
			});
		});

		// Received a message
		client.on('message', function(topic, message, pkg) {
			var data = {
				topic: topic,
				message: message,
				pkg: pkg
			};

			self.dispatcher.middleware('message', data, function() {
				self.emit('message', JSON.parse(data.message));
			});
		});
	});
};

IoT.prototype.use = function(eventName, middleware) {
	var self = this;

	// Add to middleware list
	self.middlewares[eventName].push(middleware);
};

IoT.prototype.send = function() {
	var self = this;

	var target = null;
	var collection = null;
	var endpoint = null;
	var topic = null;
	var message = null;
	
	if (argument.length == 2) {
		collection = arguments[0];
		message = arguments[1];
		target = collection;
	} else if (argument.length == 3) {
		collection = arguments[0];
		endpoint = arguments[1];
		message = arguments[2];
		target = collection + '.' + endpoint;
	} else if (argument.length == 4) {
		collection = arguments[0];
		endpoint = arguments[1];
		topic = arguments[2];
		message = arguments[3];
		target = collection + '.' + endpoint + '.' + topic;
	} else {
		throw new Error('incorrect parameters');
	}

	// Sending
	self.endpoint.client.publish(target, JSON.stringify({
		from: self.endpoint.id,
		content: message
	}));
};

IoT.prototype.sendToEndpoint = function() {
	var self = this;

	var target = null;
	var endpoint = null;
	var topic = null;
	var message = null;
	
	if (argument.length == 2) {
		endpoint = arguments[0];
		message = arguments[1];
		target = self.endpoint.collectionId + '.' + endpoint;
	} else if (argument.length == 3) {
		endpoint = arguments[0];
		topic = arguments[1];
		message = arguments[2];
		target = self.endpoint.collectionId + '.' + endpoint + '.' + topic;
	} else {
		throw new Error('incorrect parameters');
	}

	// Sending
	self.endpoint.client.publish(target, JSON.stringify({
		from: self.endpoint.id,
		content: message
	}));
};

IoT.prototype.sendToTopic = function() {
	var self = this;

	var target = null;
	var topic = null;
	var message = null;
	
	if (argument.length == 2) {
		topic = arguments[0];
		message = arguments[1];
		target = self.endpoint.collectionId + '.' + self.endpoint.id + '.' + topic;
	} else {
		throw new Error('incorrect parameters');
	}

	// Sending
	self.endpoint.client.publish(target, JSON.stringify({
		from: self.endpoint.id,
		content: message
	}));
};

IoT.Endpoint = require('./endpoint');
