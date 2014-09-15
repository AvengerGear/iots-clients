"use strict";

var crypto = require('crypto');
var events = require('events');
var util = require('util');

var Negotiator = module.exports = function(backend) {
	var self = this;

	self.backend = backend;
	self.endpoint = backend.endpoint;
	self.ContentType = {
		Plain: 0,
		JSON: 1,
		Binary: 2
	};
};

util.inherits(Negotiator, events.EventEmitter);

Negotiator.prototype.request = function(targetPath, receiver, type, data, callback) {
	var self = this;

	// Generate a message ID
	var id = Date.now() + crypto.randomBytes(16).toString('hex');

	// Response handler
	self.once(id, function(message) {

		if (message.status == 500) {
			callback(new Error('Server Internal Error'), message);
			return;
		}

		callback(null, message);
	});

	// Send request to specific topic or channel
	self.backend.publish(targetPath, {
		id: id,
		type: type,
		source: receiver,
		content: (type == self.ContentType.Plain) ? data : JSON.stringify(data)
	});

	// TODO: timeout to remove handler
};

Negotiator.prototype.handle = function(data, callback) {
	var self = this;

	var msg = null;
	try {
		msg = JSON.parse(data.message);
	} catch(e) {
		callback();
		return;
	}

	// Validate packet
	if (!self.validatePacket(msg)) {

		// Ignore this packet
		callback();
		return;
	}

	var id = msg.id;

	// No handler can deal with this message
	if (!self.listeners(id).length) {
		callback();
		return;
	}

	self.emit(id, self.parsePacket(msg), callback);
};

Negotiator.prototype.validatePacket = function(packet) {
	var self = this;

	if (!packet.id) {
		return false;
	}

	if (packet.type == undefined) {
		return false;
	}

	return true;
};

Negotiator.prototype.parsePacket = function(packet) {
	var self = this;

	// Parsing content with JSON format
	if (packet.type == self.ContentType.JSON) {
		try {
			packet.content = JSON.parse(packet.content);
		} catch(e) {
			packet.content = {};
		}
	}

	return packet;
};
