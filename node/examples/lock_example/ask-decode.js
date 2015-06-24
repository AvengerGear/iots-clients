var util = require('util');
var child_process = require('child_process');
var EventEmitter = require('events').EventEmitter;

var ASKDecode = function(options){
};

util.inherits(ASKDecode, EventEmitter);

ASKDecode.prototype.start = function() {
	var self = this;
	this.childProcess = child_process.exec("alsa-ask-decode", function(error, stdout, stderr){
		if (error) {
			console.error(error);
			return;
		}

		if (stdout) {
			self.emit("message", stdout);
		}

		self.start();
	});
};

ASKDecode.prototype.stop = function() {
	if (this.childProcess) {
		this.childProcess.kill();
	}
};

module.exports = ASKDecode;
