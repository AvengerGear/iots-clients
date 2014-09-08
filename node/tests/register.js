"use strict";

var IoT = require('../');

// Create an endpoint
var endpoint = new IoT.Endpoint();
//endpoint.service.host = 'localhost';
//endpoint.service.port = 8000;
//endpoint.mqtt.host = 'localhost';

// Register this endpoint on specific collection on server
var collectionID = '1df1ca90-275b-11e4-bfe6-85f05d846eb4';
var accessKey = '57a427907e4286e7ae46a9b5ff451539ab76bd755606c6138e7993c465036234';
endpoint.register({ collectionId: collectionID, accessKey: accessKey, host: 'localhost' }, function(err) {

	if (err) {
		console.log(err);
		return;
	}

	console.log(endpoint);

	console.log('Registered a new endpoint.');
	console.log('ID: ' + endpoint.id);
	console.log('Passphrase: ' + endpoint.passphrase);

	// Initializing IoT Network with our endpoint
	var iot = new IoT(endpoint);
	iot.on('connected', function() {
		console.log('Connected to IoT Network');
	});

	// Connect to IoT Network
	//iot.connect();
});
