
var IoT = require('../');

var iot = new IoT({
	host: 'localhost',
	endpointId: 'e9d3aaa0-2c26-11e4-8117-cf5372daf0f0',
	passphrase: 'YKE+Gfyp'
});

iot.on('connect', function(endpoint) {
	console.log('Connected to IoT network');
});
