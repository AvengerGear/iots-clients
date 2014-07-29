"use strict";

var IoT = require('../');

// Create an endpoint with ID and passphrase
var endpointID = '2fa8e470-16ef-11e4-bc34-7d5f185bcbe0';
var passphrase = 'azbycxdw';
var endpoint = new IoT.Endpoint(endpointID, passphrase);

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

// Connect to IoT Network
iot.connect();
