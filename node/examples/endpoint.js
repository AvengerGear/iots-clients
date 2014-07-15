"use strict";

var IoT = require('../');

// Create an endpoint with ID and passphrase
var collectionID = '84f80230-081c-11e4-ac59-4ba441841316';
var endpointID = '912030f0-081c-11e4-ac59-4ba441841316';
var passphrase = '';
var endpoint = new IoT.Endpoint(endpointID, passphrase);

// Initializing IoT Network with our endpoint
var iot = new IoT(endpoint);
iot.on('connected', function() {
	console.log('Connected to IoT Network');
});

iot.on('message', function(message) {
	console.log(message);
});

iot.on('private_message', function(message) {
	console.log(message);
});

// Connect to IoT Network
iot.connect();
