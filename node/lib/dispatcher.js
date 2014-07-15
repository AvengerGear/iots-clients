"use strict";

var Dispatcher = module.exports = function(iot) {
	var self = this;

	self.iot = iot;
};

Dispatcher.prototype.privateMsg = function(endpoint, message) {
	var self = this;

	switch(message.type) {
	case 'info':
		break;
	}
};

Dispatcher.prototype.middleware = function(name, data, complete) {
	var self = this;

	var handlers = self.iot.middlewares[name];
	if (handlers.length == 0)
		return;

	var index = 0;

	function next() {
		setImmediate(function() {
			index++;
			if (handlers[index])
				handlers[index](self.iot, data, next);
			else
				complete();
		});
	}

	handlers[index](self.iot, data, next);
};
