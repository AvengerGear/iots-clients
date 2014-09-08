"use strict";

var events = require('events');
var util = require('util');

var IoT = module.exports = function(options) {
	var self = this;

	self.network = new IoT.Network({
		host: options.host || 'iots.io',
		port: options.port || 1883
	});

	var endpoint = new IoT.Endpoint(options.endpointId || '', options.passphrase || '');

	self.network
		.addEndpoint(endpoint)
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
};

util.inherits(IoT, events.EventEmitter);

IoT.prototype.register = function() {
};

IoT.Network = require('./network');
IoT.Endpoint = require('./endpoint');
