var IOT = require('../../');
var Pi = require('wiring-pi');

var parseColor = function(color) {
	m = color.match(/^#([0-9a-f]{6})$/i)[1];
	if(m) {
		return [
			parseInt(m.substr(0,2),16),
			parseInt(m.substr(2,2),16),
			parseInt(m.substr(4,2),16)
		];
	}
	return undefined;
};


var led = {
	target: {r: 0, g: 0, b: 0},
	current: {r: 0, g: 0, b: 0},
	open: function(r, g, b) {
		Pi.setup('sys');
		Pi.pinMode(26, Pi.OUTPUT);
		Pi.pinMode(19, Pi.OUTPUT);
		Pi.pinMode(13, Pi.OUTPUT);
		Pi.softPwmCreate(26, 0, 255);
		Pi.softPwmCreate(19, 0, 255);
		Pi.softPwmCreate(13, 0, 255);
		this._change();
	},
	close: function(r, g, b) {
		Pi.softPwmStop(26);
		Pi.softPwmStop(19);
		Pi.softPwmStop(13);
	},
	change: function(r, g, b) {
		this.target.r = r;
		this.target.g = g;
		this.target.b = b;
	},
	_change: function() {
		var self = this;
		this.current = {
			r: this.current.r + (this.target.r - this.current.r) / 5,
			g: this.current.g + (this.target.g - this.current.g) / 5,
			b: this.current.b + (this.target.b - this.current.b) / 5,
		};
		Pi.softPwmWrite(26, Math.round(this.current.r));
		Pi.softPwmWrite(13, Math.round(this.current.g));
		Pi.softPwmWrite(19, Math.round(this.current.b));
		setTimeout(function(){self._change();}, 50);
	},
	red: function() {
		return this.change(255, 0, 0);
	},
	green: function() {
		return this.change(0, 255, 0);
	},
	blue: function() {
		return this.change(0, 0, 255);
	},
	magenta: function() {
		return this.change(255, 0, 255);
	},
	yellow: function() {
		return this.change(255, 255, 0);
	},
	cyan: function() {
		return this.change(0, 255, 255);
	},
	white: function() {
		return this.change(255, 255, 255);
	},
	off: function() {
		return this.change(0, 0, 0);
	}
};

var start = function (){
	var iot = new IOT({endpointId: '3879f470-fdd7-11e4-bad9-0577ec859bfd', passphrase: 'hello'});

	iot.on('connect', function(endpoint){
		led.green();
		console.log("connected");
	});

	iot.on('message', function(endpoint, message){
		console.log(message);
		try {
			var msg = JSON.parse(message.content);
			switch(msg.cmd) {
				case 'status':
					// endpoint.publish(message.source, JSON.stringify({color: led.hexColor}));
					endpoint.publish("953e9050-eeee-11e4-bc12-991f6f6b5e80", JSON.stringify({color: led.hexColor}));
					break;
				case 'set':
					var color = parseColor(msg.color);
					if (color) {
						console.log("cmd: set");
						led.change.apply(led, color);
						console.log(color);
						led.hexColor = msg.color;
						// endpoint.publish(message.source, JSON.stringify({color: led.hexColor}));
						endpoint.publish("953e9050-eeee-11e4-bc12-991f6f6b5e80", JSON.stringify({color: led.hexColor}));
					}
					break;
			}
		} catch (e) {
			console.error(e);
		}
	});
}

led.open();
led.magenta();
start();

process.on('SIGINT', function(){
	led.off();
	led.close();
	process.exit();
});
