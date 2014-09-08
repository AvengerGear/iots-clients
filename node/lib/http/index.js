"use strict";

var events = require('events');
var util = require('util');
var http = require('http');
var querystring = require('querystring');

var HTTPBackend = module.exports = function() {
	var self = this;

	self.client = null;
};

util.inherits(HTTPBackend, events.EventEmitter);

HTTPBackend.prototype.register = function(collectionId, accessKey, options, callback) {
	var self = this;

	// TODO: secure connection
	//
	var opts = {
		host: options.host,
		port: options.port || 80,
		secure: options.secure || false
	};

	// Preparing parameters
	var data = {
		accessKey: accessKey
	};

	var body = querystring.stringify(data);

	// Send request to service to register a new endpoint
	var req = http.request({
		hostname: opts.host,
		port: opts.port,
		path: '/apis/collection/' + collectionId,
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'Content-Length': body.length
		}
	}, function(res) {

		if (res.statusCode != 200) {
			if (callback)
				callback(new Error('Failed to register this endpoint'));

			return;
		}

		// Getting endpoint information
		res.setEncoding('utf8');
		res.on('data', function(chunk) {
			var info = JSON.parse(chunk);
			self.id = info._id;
			self.passphrase = info.passphrase;
			self.collectionId = collectionId;

			if (callback)
				callback(null, self);
		});
	});

	req.on('error', function(err) {
		callback(err);
	});

	req.write(body);
	req.end();
});

HTTPBackend.prototype.connect = function(username, password, options, callback) {
	var self = this;

	var opts = {
		host: options.host,
		port: options.port || 80,
		secure: options.secure || false,
	};

	// Preparing request
	var data = {
		passphrase: password
	};

	var body = querystring.stringify(data);

	// Authorize
	var req = http.request({
		hostname: opts.host,
		port: opts.port,
		path: '/apis/endpoint/' + username + '/auth',
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'Content-Length': body.length
		}
	}, function(res) {

		if (res.statusCode != 200) {
			if (callback)
				callback(new Error('Failed to verify this endpoint'));

			return;
		}

		// Getting endpoint information
		res.setEncoding('utf8');
		res.on('data', function(chunk) {
			var info = JSON.parse(chunk);
			self.collectionId = info._collection;

			if (callback)
				callback(null, self);
		});
	});

	req.on('error', function(err) {
		callback(err);
	});

	req.write(body);
	req.end();
};

HTTPBackend.prototype.subscribe = function(topicPath) {
	var self = this;

//	self.client.subscribe(topicPath);
};

HTTPBackend.prototype.publish = function(topicPath, packet) {
	var self = this;

//	self.client.publish(topicPath, JSON.stringify(packet));
};
