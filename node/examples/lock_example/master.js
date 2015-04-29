var util = require('util');
var fs = require('fs');
var IoT = require('../../');
var EventEmitter = require('events').EventEmitter;

var MallockClient = function(options){
	var options = options || {};
	var configPath = options.configPath || "./master-config.json";
	this.config = require(configPath);
};

util.inherits(MallockClient, EventEmitter);

MallockClient.prototype.connect = function() {
	var self = this;
	console.log("connect");

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
		self.targetEndpoint = self.config.collectionId + "/" + self.config.target;
		self.targetTopic = self.config.collectionId + "/" + self.config.target + "/" + self.config.endpointId;
		self.start();
	});
};

MallockClient.prototype.saveConfig = function() {
	fs.writeFile("./config.json", JSON.stringify(this.config, null, "\t"), function(err){
		if (err){
			console.error("Error on writing config");
		}
		console.log("configuration updated");
	});
};

MallockClient.prototype.start = function() {
	var self = this;
	this.endpoint.publish(this.targetEndpoint, "authorize");
	this.iot.on('message', function(endpoint, message, topic) {
		var srcEndpoint = message.source.split("/")[1];
		if (srcEndpoint === self.config.endpointId) {
			return;
		}
		if (message.source === self.targetEndpoint && message.content === "authorized") {
			console.log("Authorized. Subscribing topic.");
			self.endpoint.subscribe(self.targetTopic, {}, function(err) {
				console.log("Topic subscribed. Sending message.");
				if (err) {
					console.error(err);
					return;
				}
				self.endpoint.publish(self.targetTopic, JSON.stringify({command: "unlock"}));
			});
		} else if (message.source === self.targetEndpoint && message.content === "acknowledged") {
			console.log("Endpoint acknowledged. Waiting for master's reply.");
		} else if (topic === self.targetTopic) {
			try {
				self.runCommand(message.source, JSON.parse(message.content));
			} catch (e) {
				console.error(e);
			}
		}
	});
};

MallockClient.prototype.commands = {
	unlocked: function(src, opt) {
		console.log("DEVICE UNLOCKED");
	},
	requestAuthorization: function(src, opt) {
		console.log("Authorization requested from %s to %s", opt.requestEndpoint, opt.targetEndpoint);
		console.log("Sending authorization...");
		this.endpoint.publish(this.targetTopic, JSON.stringify({
			command: "authorize",
			options: {
				requestEndpoint: opt.requestEndpoint,
				targetEndpoint: opt.targetEndpoint
			}
		}));
	}
}

MallockClient.prototype.runCommand = function(src, cmd){
	var self = this;
	if (!(cmd.command in self.commands)){
		console.error("Channel command invalid");
		return;
	}
	self.commands[cmd.command].apply(self, [src, cmd.options]);
}

client = new MallockClient();
client.connect();
