"use strict";

var IoT = require('../');

// Create an endpoint with ID and passphrase
var endpointID = 'e9d3aaa0-2c26-11e4-8117-cf5372daf0f0';
var passphrase = 'YKE+Gfyp';
var endpoint = new IoT.Endpoint(endpointID, passphrase);
endpoint.service.host = 'localhost';
endpoint.service.port = 8000;
endpoint.mqtt.host = 'localhost';

// Initializing IoT Network with our endpoint
var iot = new IoT(endpoint);
iot.on('connected', function() {
	console.log('Connected to IoT Network');
});

iot.on('error', function(err) {
	console.log(err);
});

iot.on('message', function(message) {
	console.log(message);
});

iot.on('private_message', function(message) {
	console.log(message);
});

iot.on('topic', function(topic, message) {
	console.log('TOPIC', topic);
});

// Connect to IoT Network
iot.connect();
