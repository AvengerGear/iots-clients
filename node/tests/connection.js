
var IoT = require('../');

// Create an endpoint with ID and passphrase
var endpointID = 'e9d3aaa0-2c26-11e4-8117-cf5372daf0f0';
var passphrase = 'YKE+Gfyp';
var endpoint = new IoT.Endpoint(endpointID, passphrase);

var network = new IoT.Network({ host: 'localhost' });
network.addEndpoint(endpoint);

network.on('connect', function(endpoint) {
	console.log('Connected to IoT network');
});
