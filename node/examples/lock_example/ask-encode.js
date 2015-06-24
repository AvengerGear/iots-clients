var util = require('util');
var child_process = require('child_process');
var EventEmitter = require('events').EventEmitter;

var ASKEncode = function(options){
};

util.inherits(ASKEncode, EventEmitter);

ASKEncode.prototype.encode = function(payload) {
	var self = this;
	if (this.childProcess) {
		throw new Error("The audio is playing");
	}
	this.childProcess = child_process.spawn("alsa-ask-encode");
	this.childProcess.on('close', function(code) {
		delete self.childProcess;
		self.emit("complete");
	});
	this.childProcess.stdin.write(payload);
	this.childProcess.stdin.end();
};

ASKEncode.prototype.stop = function() {
	if (this.childProcess) {
		this.childProcess.kill();
		delete this.childProcess;
	}
};

module.exports = ASKEncode;
