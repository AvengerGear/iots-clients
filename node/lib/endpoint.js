"use strict";

var http = require('http');
var querystring = require('querystring');
var mqtt = require('mqtt');

var Endpoint = module.exports = function(id, passphrase) {
	var self = this;

	self.service = {
		host: 'iots.io',
		port: 8000,
		secure: false
	};

	self.mqtt = {
		host: 'iots.io',
		port: 1883
	};

	self.client = null;
	self.id = id || null;
	self.passphrase = passphrase || '';
	self.collectionId = null;
};

Endpoint.prototype.register = function(collectionId, accessKey, complete) {
	var self = this;

	// TODO: secure connection

	// Preparing request
	var data = {
		accessKey: accessKey
	};

	var body = querystring.stringify(data);

	// Send request to service to register a new endpoint
	var req = http.request({
		hostname: self.service.host,
		port: self.service.port,
		path: '/apis/collection/' + collectionId,
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'Content-Length': body.length
		}
	}, function(res) {

		if (res.statusCode != 200) {
			if (complete)
				complete(new Error('Failed to register this endpoint'));

			return;
		}

		// Getting endpoint information
		res.setEncoding('utf8');
		res.on('data', function(chunk) {
			var info = JSON.parse(chunk);
			self.id = info._id;
			self.passphrase = info.passphrase;
			self.collectionId = collectionId;

			if (complete)
				complete(null, self);
		});
	});

	req.on('error', function(err) {
		complete(err);
	});

	req.write(body);
	req.end();
};

Endpoint.prototype.auth = function(opts, complete) {
	var self = this;

	// TODO: secure connection

	// Preparing request
	var data = {
		passphrase: self.passphrase
	};

	var body = querystring.stringify(data);

	// Authorize
	var req = http.request({
		hostname: self.service.host,
		port: self.service.port,
		path: '/apis/endpoint/' + self.id + '/auth',
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded',
			'Content-Length': body.length
		}
	}, function(res) {

		if (res.statusCode != 200) {
			if (complete)
				complete(new Error('Failed to verify this endpoint'));

			return;
		}

		// Getting endpoint information
		res.setEncoding('utf8');
		res.on('data', function(chunk) {
			var info = JSON.parse(chunk);
			self.collectionId = info._collection;

			if (complete)
				complete(null, self);
		});
	});

	req.on('error', function(err) {
		complete(err);
	});

	req.write(body);
	req.end();
};

Endpoint.prototype.createConnection = function() {
	var self = this;

	// Create a connection
	var client = self.client = mqtt.createClient(self.mqtt.port, self.mqtt.host, {
		clientId: self.id,
		username: self.id,
		password: self.passphrase
	});

	return client;
};
