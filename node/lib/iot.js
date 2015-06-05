"use strict";

var events = require('events');
var util = require('util');

var IoT = module.exports = function(options) {
	var self = this;

	self.network = new IoT.Network({
		host: options.host || 'iots.io',
		port: options.port || 1883
	});

	self.network
		.on('connect', function() {
			var args = [ 'connect' ].concat(Array.prototype.slice.call(arguments));
			self.emit.apply(self, args);
		})
		.on('message', function() {
			var args = [ 'message' ].concat(Array.prototype.slice.call(arguments));
			self.emit.apply(self, args);
		})
		.on('error', function() {
			var args = [ 'error' ].concat(Array.prototype.slice.call(arguments));
			self.emit.apply(self, args);
		})
		.on('close', function() {
			var args = [ 'close' ].concat(Array.prototype.slice.call(arguments));
			self.emit.apply(self, args);
		});

	var endpoint = null;
	var endpointId = options.endpointId || null;
	var passphrase = options.passphrase || null;

	// Not yet register
	if (!endpointId && !endpointId && options.collectionId) {

		// Create a new endpoint then register it
		endpoint = new IoT.Endpoint();
		endpoint.register({
			collectionId: options.collectionId,
			accessKey: options.accessKey || '',
			host: self.network.host,
			port: self.network.port,
			backendType: self.network.backendType
		}, function(err) {

			if (err) {
				self.emit('error', err);
				return;
			}

			self.emit('registered', endpoint);

			// Add this new endpoint to network and establish connection
			self.network.addEndpoint(endpoint);
		});

		return;
	}

	// Created a default endpoint
	var endpoint = new IoT.Endpoint(options.endpointId || '', options.passphrase || '');
	self.network.addEndpoint(endpoint);
};

util.inherits(IoT, events.EventEmitter);

IoT.Network = require('./network');
IoT.Endpoint = require('./endpoint');
