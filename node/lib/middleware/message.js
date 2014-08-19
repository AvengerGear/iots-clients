"use strict";

var message = module.exports = function(iot, data, next) {

	// System
	if (data.topic == iot.channels.systemChannel) {
		iot.emit('system', JSON.parse(data.message));
		return;
	}

	// Collection
	if (data.topic == iot.channels.collectionChannel) {
		iot.emit('collection', JSON.parse(data.message));
		return;
	}

	// Endpoint
	if (data.topic == iot.channels.endpointChannel) {
		iot.emit('private_message', JSON.parse(data.message));
		return;
	}

	next();
};
