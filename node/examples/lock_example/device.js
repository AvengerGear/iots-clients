var util = require('util');
var fs = require('fs');
var IoT = require('../../');
var EventEmitter = require('events').EventEmitter;
var ASKDecode = require('./ask-decode');

var DemoDevice = function(options){
	var options = options || {};
	var configPath = options.configPath || "./device-config.json";
	this.config = require(configPath);
	this.askdecode = new ASKDecode();
};

util.inherits(DemoDevice, EventEmitter);

DemoDevice.prototype.connect = function() {
	var self = this;
	console.log("connect");

	this.askdecode.on('message', function(message){
		console.log("Received via Audio: %s", message);
		try {
			var msg = JSON.parse(message);
			// {s: "source", p: "pair code"}
			var src = msg.s;
			var pairCode = msg.p;
			console.log("Received pair request from %s via audio", src);
			console.log("Pair code: %s", pairCode);
			this.endpoint.publish(this.config.collectionId + "/" + src + "/" + pairCode, JSON.stringify({
				command: "pair",
				options: {
					pairCode: pairCode
				}
			}));
		} catch (e) {
			console.error(e);
		}
	});

	this.iot = new IoT(this.config);

	this.iot.on('registered', function(endpoint) {
		console.log("registered");
		self.config.endpointId = endpoint.id;
		self.config.passphrase = endpoint.passphrase;
		self.saveConfig();
	});

	this.iot.on('connect', function(endpoint) {
		console.log("connected");
		self.endpoint = endpoint;
		self.emit('connected', endpoint);
		endpoint.createTopic(self.config.master, {}, function(err) {
			if (err) {
				console.log(err);
			}
			self.endpointPath = self.config.collectionId + "/" + self.config.endpointId;
			endpoint.subscribe(self.endpointPath + "/" + self.config.master, {}, function(err) {
				console.log("start");
				self.askdecode.start();
				self.start();
			});
		});
	});
};

DemoDevice.prototype.saveConfig = function() {
	fs.writeFile("./device-config.json", JSON.stringify(this.config, null, "\t"), function(err){
		if (err){
			console.error("Error on writing config");
		}
		console.log("configuration updated");
	});
};

DemoDevice.prototype.start = function() {
	var self = this;
	this.topicDispatcher = {};
	this.topicDispatcher[self.endpointPath] = function(srcEndpoint, message) {
		if (message.content === "authorize") {
			if (self.isAllowing(srcEndpoint)) { // If the endpoint is allowed
				console.log("%s: authorized", srcEndpoint);
				var srcEndpointPath = self.endpointPath + "/" + srcEndpoint;
				self.endpoint.subscribe(srcEndpointPath, {}, function(err){
					self.topicDispatcher[srcEndpointPath] = self.topicHandlerFactory(srcEndpointPath);
					self.endpoint.publish(self.config.collectionId + "/" + srcEndpoint, "authorized");
				});
			} else {
				console.log("%s: requested for master authorization", srcEndpoint);
				self.endpoint.publish(self.config.collectionId + "/" + srcEndpoint, "acknowledged");
				self.endpoint.publish(self.endpointPath + "/" + self.config.master, JSON.stringify({
					command: "requestAuthorization",
					options: {
						requestEndpoint: srcEndpoint,
						targetEndpoint: self.config.endpointId
					}
				}));
			}
		}
	};
	this.iot.on('message', function(endpoint, message, topic) {
		var srcEndpoint = message.source.split("/")[1];
		if (srcEndpoint === self.config.endpointId) {
			return;
		}
		if (topic in self.topicDispatcher) {
			self.topicDispatcher[topic](srcEndpoint, message);
		}
	});
}

DemoDevice.prototype.commands = {
	// TODO: change to the raspberry pi demo
	unlock: function(src, opt) {
		console.log("DEVICE UNLOCKED");
		this.endpoint.publish(this.endpointPath + "/" + src, JSON.stringify({
			command: "unlocked"
		}));
	},
	authorize: function(src, opt) {
		var self = this;
		if (opt.targetEndpoint !== self.config.endpointId) {
			console.error("Authorize error: endpointId does not match");
			return;
		}
		console.log("Authorization accepted from %s to %s", opt.requestEndpoint, opt.targetEndpoint);
		self.endpoint.createTopic(opt.requestEndpoint, {}, function(err) {
			if (err) {
				console.log(err);
			}
			var srcEndpointPath = self.endpointPath + "/" + opt.requestEndpoint;
			self.endpoint.subscribe(srcEndpointPath, {}, function(err) {
				self.topicDispatcher[srcEndpointPath] = self.topicHandlerFactory(srcEndpointPath);
				self.endpoint.publish(self.config.collectionId + "/" + opt.requestEndpoint, "authorized");
			});
		});
	}
}

DemoDevice.prototype.runCommand = function(src, cmd){
	var self = this;
	if (!(cmd.command in self.commands)){
		console.error("Channel command invalid");
		return;
	}
	self.commands[cmd.command].apply(self, [src, cmd.options]);
}

DemoDevice.prototype.topicHandlerFactory = function(topic) {
	var self = this;
	return function(srcEndpoint, message){
		self.runCommand(srcEndpoint, JSON.parse(message.content));
	};
}


DemoDevice.prototype.isAllowing = function(endpoint) {
	if (endpoint === this.config.master || this.config.authorizedEndpoints.indexOf(endpoint) >= 0) {
		return true;
	}
	return false;
}

client = new DemoDevice();
client.connect();
