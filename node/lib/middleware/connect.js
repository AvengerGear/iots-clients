"use strict";

var connect = module.exports = function(network, endpoint, next) {

	// Subscribe to private channel to receive messages from server
	endpoint.backend.subscribe('private/' + endpoint.id, function() {

		// Subscribe basic channel and topic
		endpoint.backend.subscribe('00000000-0000-0000-0000-000000000000', function() {

			// Getting endpoint information from server
			endpoint.backend.systemCall('Endpoint', 1, {
				cmd: 'Auth',
				passphrase: endpoint.passphrase
			}, function(err, packet) {
				// Getting collection ID
				endpoint.collectionId = packet.content._collection;

				// Subscribe to collection and own channels
				endpoint.backend.subscribe(endpoint.collectionId, function() {
					endpoint.backend.subscribe(endpoint.collectionId + '/' + endpoint.id, function() {
						next();
					});
				});

			});
		});
	});
};
