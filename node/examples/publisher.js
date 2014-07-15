"use strict";

var IoT = require('../');

// Create an endpoint with ID and passphrase
var collectionID = '84f80230-081c-11e4-ac59-4ba441841111';
var endpointID = '912030f0-081c-11e4-ac59-4ba441841111';
var passphrase = '';
var endpoint = new IoT.Endpoint(endpointID, passphrase);

// Initializing IoT Network with our endpoint
var iot = new IoT(endpoint);
iot.on('connected', function() {
	console.log('Connected to IoT Network');

	iot.send('912030f0-081c-11e4-ac59-4ba441841316', 'Test');
	iot.publish('912030f0-081c-11e4-ac59-4ba441841316', 'Test');
});

iot.use('message', function(endpoint, data, next) {
	//console.log(data);
});

// Connect to IoT Network
iot.connect();
