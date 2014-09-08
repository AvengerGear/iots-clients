"use strict";

var events = require('events');
var util = require('util');
var amqp = require('amqp');

var RuleEngine = module.exports = function() {
	var self = this;

	self.service = {
		host: 'localhost',
		port: 5672,
		secure: false
	};
	self.collectionId = null;
	self.accessKey = null;
	self.connection = null;
	self.queue = null;
	self.middlewares = [];
};

util.inherits(RuleEngine, events.EventEmitter);

RuleEngine.prototype.connect = function(collectionId, accessKey, opts) {
	var self = this;

	var connection = self.connection = amqp.createConnection({ host: self.service });

	// Wait for connection to become established.
	connection.on('ready', function () {
		self.collectionId = collectionId;
		self.accessKey = accessKey;

		self.emit('connected', connection);

		// Use the default 'amq.topic' exchange
		connection.queue(collectionId, function(q){
			self.queue = q;

/*
			// Catch all messages
			q.bind('#');

			// Receive messages
			q.subscribe(function(message) {
					// Print messages to stdout
					console.log(message);
			});

			connection.publish('my-queue', 'TEST');
*/
			self.emit('ready', q);
		});
	});
};

RuleEngine.prototype.use = function(middleware) {
	var self = this;

	// Add to middleware list
	self.middlewares.push(middleware);
};

RuleEngine.prototype.subscribe = function(topic, handler) {
	var self = this;

	self.queue.subscribe();
};
