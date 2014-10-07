"use strict";

var events = require('events');
var util = require('util');
var Dispatcher = require('./dispatcher');

var Network = module.exports = function(options) {
	var self = this;

	var opts = options || {};

	self.host = opts.host || 'iots.io';
	self.port = opts.port || 1883;
	self.backendType = 'mqtt';
	self.endpoints = [];
	self.dispatcher = new Dispatcher(self);
	self.middlewares = {
		connect: [
			require('./middleware/connect'),
		],
		message: [
			require('./middleware/message')
		]
	};
};

util.inherits(Network, events.EventEmitter);

Network.prototype.addEndpoint = function(endpoint) {
	var self = this;

	self.endpoints.push(endpoint);

	// Establishing connection
	endpoint.createConnection({
		host: self.host,
		port: self.port,
		backendType: self.backendType
	}, function(err) {

		endpoint.on('connect', function() {
			self.dispatcher.middleware('connect', endpoint, function() {
				self.emit('connect', endpoint);
			});
		});

		endpoint.on('close', function() {
			self.emit('close', endpoint);
		});

		endpoint.on('error', function() {
			self.emit('error', endpoint);
		});

		endpoint.on('message', function(topic, message) {
			var data = {
				endpoint: endpoint,
				topic: topic,
				message: message
			};

			self.dispatcher.middleware('message', data, function() {
				self.emit('message', endpoint, JSON.parse(data.message));
			});
		});
	});

	return self;
};
