var IoT = require('../');

var iot = new IoT({
	host: 'localhost',
	endpointId: '1b639080-6944-11e4-aa45-03869b8b3dc9',
	passphrase: '1234'
});

iot.on('connect', function(endpoint) {
	console.log('Connected to IoT network');

	// Subscribe to specific topic
	endpoint.publish('e228a8c0-68f1-11e4-92bb-4942cb7d7657', "Hello from Node.JS Endpoint");
});
