var util = require('util');
var fs = require('fs');
var IoT = require('../../');
var EventEmitter = require('events').EventEmitter;
var ASKEncode = require('./ask-encode');

var DemoMaster = function(options){
	var options = options || {};
	var configPath = options.configPath || "./master-config.json";
	this.config = require(configPath);
	this.askencode = new ASKEncode();
};

util.inherits(DemoMaster, EventEmitter);

DemoMaster.prototype.connect = function() {
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
		self.endpointPath = self.config.collectionId + "/" + self.config.endpointId;
		self.start();
	});
};

DemoMaster.prototype.saveConfig = function() {
	fs.writeFile("./master-config.json", JSON.stringify(this.config, null, "\t"), function(err){
		if (err){
			console.error("Error on writing config");
		}
		console.log("configuration updated");
	});
};

DemoMaster.prototype.start = function() {
	var self = this;
	this.iot.on('message', function(endpoint, message, topic) {
		console.log(message);
		var srcEndpoint = message.source.split("/")[1];
		if (srcEndpoint === self.config.endpointId) {
			return;
		}
		if (message.content === "authorized") {
			console.log("Authorized. Subscribing topic.");
			var targetTopic = message.source + "/" + self.config.endpointId;
			self.endpoint.subscribe(targetTopic, {}, function(err) {
				console.log("Topic subscribed. Sending message.");
				if (err) {
					console.error(err);
					return;
				}
				// TODO: change to the raspberry pi demo
				self.endpoint.publish(targetTopic, JSON.stringify({command: "unlock"}));
			});
		} else if (message.content === "acknowledged") {
			console.log("Endpoint acknowledged. Waiting for master's reply.");
		} else if (self.pairTopicPath && topic === self.pairTopicPath) {
			var opt = JSON.parse(message.content);
			console.log("Received pair request from %s", message.source);
			console.log("Pair code: %s", opt.pairCode);
			if (self.pairCode === opt.pairCode) {
				self.endpoint.publish(src, "authorize");
			} else {
				console.log("Pair codes do not match");
			}
		} else {
			try {
				self.runCommand(message.source, JSON.parse(message.content));
			} catch (e) {
				console.error(e);
			}
		}
	});
	this.pair();
};

DemoMaster.prototype.commands = {
	unlocked: function(src, opt) {
		console.log("DEVICE UNLOCKED");
	},
	requestAuthorization: function(src, opt) {
		console.log("Authorization requested from %s to %s", opt.requestEndpoint, opt.targetEndpoint);
		console.log("Sending authorization...");
		this.endpoint.publish(opt.targetEndpoint, JSON.stringify({
			command: "authorize",
			options: {
				requestEndpoint: opt.requestEndpoint,
				targetEndpoint: opt.targetEndpoint
			}
		}));
	}
}

DemoMaster.prototype.runCommand = function(src, cmd){
	var self = this;
	if (!(cmd.command in self.commands)){
		console.error("Channel command invalid");
		return;
	}
	self.commands[cmd.command].apply(self, [src, cmd.options]);
};

DemoMaster.prototype.pair = function(){
	var self = this;
	if (this.pairCode) {
		this.endpoint.unsubscribe(this.endpointPath + "/" + this.pairCode.toString());
	}
	this.pairCode = Math.floor(Math.random()*10000000);
	console.log("Pair code generated: %s", this.pairCode);

	// create a topic -> subscribe the created topic -> send message via sound -> wait for reply
	this.endpoint.createTopic(this.pairCode.toString(), {}, function(err) {
		self.pairTopicPath = self.endpointPath + "/" + self.pairCode.toString();
		self.endpoint.subscribe(self.pairTopicPath, {}, function(err) {
			var msg = JSON.stringify({s: self.config.endpointId, p: self.pairCode});
			console.log(msg);
			self.askencode.encode(msg);
		});
	});
};

client = new DemoMaster();
client.connect();
