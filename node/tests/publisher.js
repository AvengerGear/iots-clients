"use strict";

var IoT = require('../');

// Create an endpoint with ID and passphrase
var endpointID = '42c31540-2c29-11e4-8117-cf5372daf0f0';
var passphrase = 'Ngl9iVMl';
var endpoint = new IoT.Endpoint(endpointID, passphrase);
endpoint.service.host = 'localhost';
endpoint.service.port = 8000;
endpoint.mqtt.host = 'localhost';

// Initializing IoT Network with our endpoint
var iot = new IoT(endpoint);
/*
iot.createTopic('chatroom', { secretKey: 'ABC' }, function(err, topicId) {
	iot.sendToEndpoint('e9d3aaa0-2c26-11e4-8117-cf5372daf0f0', 'invite', {
		topic: endpoint.collectionId + '.' + endpoint.id + '.chatroom',
		secretKey: 'ABC'
	});
});
*/
iot.on('connected', function() {
	console.log('Connected to IoT Network');

	// Send private message to specific endpoint
	//iot.sendToEndpoint('e9d3aaa0-2c26-11e4-8117-cf5372daf0f0', 'chatroom', 'Hello');
	iot.endpoint.client.publish('1df1ca90-275b-11e4-bfe6-85f05d846eb4/e9d3aaa0-2c26-11e4-8117-cf5372daf0f0', 'Hello');
});

iot.on('error', function(err) {
	console.log(err);
});

iot.use('message', function(endpoint, data, next) {
	//console.log(data);
});

// Connect to IoT Network
iot.connect();
