
var IoT = require('../');

var iot = new IoT({
	host: 'localhost',
	endpointId: 'e9d3aaa0-2c26-11e4-8117-cf5372daf0f0',
	passphrase: 'YKE+Gfyp'
});

iot.on('connect', function(endpoint) {
	console.log('Connected to IoT network');

	// Query myself
	endpoint.queryTopic('e9d3aaa0-2c26-11e4-8117-cf5372daf0f0', {}, function(err, topics) {
		if (err) {
			console.log(err);
			return;
		}

		console.log(topics);
	});

	// Subscribe to specific topic
	endpoint.subscribe('1df1ca90-275b-11e4-bfe6-85f05d846eb4/e9d3aaa0-2c26-11e4-8117-cf5372daf0f0/myTopic', {}, function(err) {
		console.log(err);
	});

});

iot.on('message', function(endpoint, message) {
	console.log('Received ', message);
});
