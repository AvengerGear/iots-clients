"use strict";

var IoT = require('../');

// Create an endpoint
var endpoint = new IoT.Endpoint();

// Register this endpoint on specific collection on server
var collectionID = '84f80230-081c-11e4-ac59-4ba441841316';
var accessKey = '234b90e4c00b76b3cb354c27b3edb8e1b86d173fecf8c9d8e4e7980e1c668bb0';
endpoint.register(collectionID, accessKey, function(err, endpoint) {

	if (err) {
		console.log(err);
		return;
	}

	console.log('Registered a new endpoint.');
	console.log('ID: ' + endpoint.id);
	console.log('Passphrase: ' + endpoint.passphrase);

	// Initializing IoT Network with our endpoint
	var iot = new IoT(endpoint);
	iot.on('connected', function() {
		console.log('Connected to IoT Network');
	});

	// Connect to IoT Network
	iot.connect();
});
