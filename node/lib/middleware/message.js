"use strict";

var message = module.exports = function(iot, data, next) {

	// Private message
	if (data.topic == iot.channels.privateChannel) {
		iot.emit('private_message', JSON.parse(data.message));
		return;
	}

	next();
};
