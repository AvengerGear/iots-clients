"use strict";

var Dispatcher = module.exports = function(network) {
	var self = this;

	self.network = network;
};

Dispatcher.prototype.middleware = function(name, data, complete) {
	var self = this;

	var handlers = self.network.middlewares[name];
	if (handlers.length == 0)
		return;

	var index = 0;

	function next() {
		setImmediate(function() {
			index++;
			if (handlers[index])
				handlers[index](self.network, data, next);
			else
				complete();
		});
	}

	handlers[index](self.network, data, next);
};
