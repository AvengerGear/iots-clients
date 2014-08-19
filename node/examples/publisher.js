"use strict";

var IoT = require('../');

// Create an endpoint with ID and passphrase
var endpointID = '912030f0-081c-11e4-ac59-4ba441841316';
var passphrase = '';
var endpoint = new IoT.Endpoint(endpointID, passphrase);

// Initializing IoT Network with our endpoint
var iot = new IoT(endpoint);
iot.on('connected', function() {
	console.log('Connected to IoT Network');

	// Send private message to specific endpoint
	iot.sendToEndpoint('2fa8e470-16ef-11e4-bc34-7d5f185bcbe0', 'Test');
});

iot.on('error', function(err) {
	console.log(err);
});

iot.use('message', function(endpoint, data, next) {
	//console.log(data);
});

// Connect to IoT Network
iot.connect();
