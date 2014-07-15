"use strict";

var events = require('events');
var util = require('util');
var Dispatcher = require('./dispatcher');

var IoT = module.exports = function(endpoint) {
	var self = this;

	self.endpoint = endpoint;
	self.dispatcher = new Dispatcher(self);
	self.channels = [];
	self.middlewares = {
		connect: [
			require('./middleware/connect')
		],
		message: [
			require('./middleware/message')
		]
	};
};

util.inherits(IoT, events.EventEmitter);

IoT.prototype.connect = function() {
	var self = this;

	// Setup channels
	self.channels.publicChannel = self.endpoint.id;
	self.channels.privateChannel = 'private/' + self.endpoint.id;

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
};

IoT.prototype.use = function(eventName, middleware) {
	var self = this;

	// Add to middleware list
	self.middlewares[eventName].push(middleware);
};

IoT.prototype.send = function(target, message) {
	var self = this;

	self.endpoint.client.publish('private/' + target, JSON.stringify({
		from: self.endpoint.id,
		content: message
	}));
};

IoT.prototype.publish = function(target, message) {
	var self = this;

	self.endpoint.client.publish(target, JSON.stringify({
		from: self.endpoint.id,
		content: message
	}));
};

IoT.Endpoint = require('./endpoint');
