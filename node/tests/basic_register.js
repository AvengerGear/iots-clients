
var IoT = require('../');

var iot = new IoT({
	host: 'localhost',
	collectionId: '1df1ca90-275b-11e4-bfe6-85f05d846eb4',
	accessKey: '57a427907e4286e7ae46a9b5ff451539ab76bd755606c6138e7993c465036234'
});

iot.on('registered', function(endpoint) {
	console.log('Registered a new endpoint.');
	console.log('Endpoint ID:', endpoint.id);
	console.log('Passphrase:', endpoint.passphrase);
});

iot.on('connect', function(endpoint) {
	console.log('Connected to IoT network');
});
