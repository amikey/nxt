/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $) {
    var requestConfirmations = [];

    NRS.updateRemoteNodes = function() {
        var data = {state: "CONNECTED", includePeerInfo: true};
        NRS.sendRequest("getPeers", data, function (response) {
            if (response.peers) {
                NRS.remoteNodesMgr.setRemoteNodes(response.peers);
            }
            setTimeout(function () {
                NRS.updateRemoteNodes();
            }, 30000);
        });
    };

    NRS.initRemoteNodesMgr = function(isTestnet, isMobileApp) {
        NRS.remoteNodesMgr = new RemoteNodesManager(isTestnet, isMobileApp);
        if (!isMobileApp) {
            NRS.updateRemoteNodes();
        }
    };

    NRS.requestNeedsConfirmation = function (requestType) {
        var plusIndex = requestType.indexOf("+");
        if (plusIndex > 0) {
            requestType = requestType.substring(0, plusIndex);
        }
        return !NRS.isRequirePost(requestType) && NRS.isRequestForwardable(requestType)
    };

    NRS.toNormalizedResponseString = function (response) {
        var responseWithoutProcTime = {};
        for (var key in response) {
            if (response.hasOwnProperty(key) && key != "requestProcessingTime") {
                responseWithoutProcTime[key] = response[key];
            }
        }
        return JSON.stringify(responseWithoutProcTime);
    };

    NRS.confirmRequest = function(requestType, data, expectedResponse, requestRemoteNode) {
        if (NRS.requestNeedsConfirmation(requestType)) {
            var expectedResponseStr = NRS.toNormalizedResponseString(expectedResponse);

            var ignoredAddresses = [];
            if (requestRemoteNode) {
                ignoredAddresses.push(requestRemoteNode.address);
            }
            var nodes = NRS.remoteNodesMgr.getRandomNodes(3, ignoredAddresses);
            var confirmationReport = {processing: [], confirmations: [], rejections: []};
            requestConfirmations.unshift(confirmationReport);
            if (requestConfirmations.length > 10) {
                requestConfirmations.pop();
            }
            function onConfirmation(response) {
                var fromNode = this;
                NRS.logConsole("Confirm with node " + fromNode.announcedAddress);
                var index = confirmationReport.processing.indexOf(fromNode.announcedAddress);
                confirmationReport.processing.splice(index, 1);

                if (response.errorCode && response.errorCode == -1) {
                    // Confirmation request received a network error
                    // Retry the request on another node until all nodes are ignored
                    var retryNode = NRS.remoteNodesMgr.getRandomNode(ignoredAddresses);
                    if (retryNode != null) {
                        NRS.logConsole("Retry node " + retryNode.announcedAddress);
                        confirmationReport.processing.push(retryNode.announcedAddress);
                        ignoredAddresses.push(retryNode.address);
                        retryNode.sendRequest(requestType, data, onConfirmation);
                    }
                } else {
                    var responseStr = NRS.toNormalizedResponseString(response);
                    if (responseStr == expectedResponseStr) {
                        confirmationReport.confirmations.push(fromNode.announcedAddress);
                    } else {
                        NRS.logConsole(fromNode.announcedAddress + " response defers from " + requestRemoteNode.announcedAddress + " response for " + requestType);
                        NRS.logConsole("Expected Response: " + expectedResponseStr);
                        NRS.logConsole("Actual   Response: " + responseStr);
                        confirmationReport.rejections.push(fromNode.announcedAddress);
                    }

                    if (confirmationReport.processing.length == 0) {
                        NRS.logConsole("Request " + requestType + " confirmed by " + confirmationReport.confirmations.length + " times");
                    }
                }
            }

            $.each(nodes, function (index, node) {
                confirmationReport.processing.push(node.announcedAddress);
                ignoredAddresses.push(node.address);
                node.sendRequest(requestType, data, onConfirmation);
            });
        }
    };

	return NRS;
}(NRS || {}, jQuery));