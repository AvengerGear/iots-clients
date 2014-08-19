"use strict";

var connect = module.exports = function(iot, client, next) {

	// Subscribe to system channel
	client.subscribe(iot.channels.systemChannel);

	// Subscribe to collection channel
	client.subscribe(iot.channels.collectionChannel);

	// Subscribe to endpoint channel
	client.subscribe(iot.channels.endpointChannel);

	next();
};
