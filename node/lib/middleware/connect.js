"use strict";

var connect = module.exports = function(iot, client, next) {

	// Subscribe to public channel
	client.subscribe(iot.channels.publicChannel);

	// Subscribe to private channel
	client.subscribe(iot.channels.privateChannel);

	// Subscribe to collection channel
	client.subscribe(iot.channels.collectionChannel);

	next();
};
