'use strict';

function displayController($rootScope, $scope, $compile, $http) {
    let ctrl = this;
    ctrl.reportDetails = {text: "", values: {}, notifications: {}};
    ctrl.stubStrategySelect = "";
    ctrl.availableStorages = {'testStorage': 'Test'};
    ctrl.displayingReport = false;
    ctrl.id = Math.random().toString(36).substring(7);
    ctrl.editing = false;
    ctrl.diff = [];

    ctrl.rerun = function () {
        let data = {};
        let storageId = ctrl.selectedNode["ladybug"]["storageId"];
        data[$scope.storage] = [];
        console.info("Rerunning the report with storage id", storageId);
        $http.post("../runner/run/" + ctrl.storage, data)
            .then(function (response) {
                console.debug("Rerun is successful", response);
            }, function (response) {
                let title = "Rerun failed!";
                if (response.status === 401) title = "Method not allowed!";
                createToast(title, "Rerun of the report with storage id [" + storageId + "] failed.",
                    $scope, $compile);
                console.error(title, response);
            });
    };

    ctrl.toggleEdit = function () {
        if (ctrl.editing) {
            let buttonText = 'Read-only';
            $('#details-edit' + ctrl.id).text(buttonText);
        }

        if (ctrl.isReportNode()) {
            ctrl.toggleEditReport();
        } else {
            ctrl.toggleEditCheckpoint();
        }
    }

    ctrl.toggleEditReport = function () {
        console.info("Toggling edit for report");
        let originalTransformation = "";
        if ("transformation" in ctrl.reportDetails.data && ctrl.reportDetails.data.transformation)
            originalTransformation = ctrl.reportDetails.data.transformation;
        let originalVariables = "";
        if ("variables" in ctrl.reportDetails.data && ctrl.reportDetails.data.variables)
            originalVariables = ctrl.reportDetails.data.variables;

        if (!ctrl.editing) {
            ctrl.editing = true;

            require(['vs/editor/editor.main'], function () {
                ctrl.editor = {
                    transformation: monaco.editor.create(document.getElementById('editorTransformation' + ctrl.id), {
                        value: originalTransformation,
                        language: 'xml',
                        fontSize: "10px",
                        minimap: {
                            enabled: false
                        }
                    }),
                    variable: monaco.editor.create(document.getElementById('editorVariables' + ctrl.id), {
                        value: originalVariables,
                        language: 'csv',
                        fontSize: "10px",
                        minimap: {
                            enabled: false
                        }
                    }),
                };

                if($scope.$root.$$phase !== '$apply' && $scope.$root.$$phase !== '$digest') $scope.$apply();
                ctrl.editor.variable.layout();
                ctrl.editor.transformation.layout();
            });
            $("#editName" + ctrl.id).val(ctrl.reportDetails.data.name);
            $("#editPath" + ctrl.id).val(ctrl.reportDetails.data.path);
            $("#editDescription" + ctrl.id).val(ctrl.reportDetails.data.description);
        } else {
            let dmp = new diff_match_patch();
            ctrl.diff = [];
            ctrl.addToDiff(dmp, ctrl.reportDetails.data.name, $("#editName" + ctrl.id).val(), "Name:");
            ctrl.addToDiff(dmp, ctrl.reportDetails.data.path, $("#editPath" + ctrl.id).val(), "Path:");
            ctrl.addToDiff(dmp, ctrl.reportDetails.data.description, $("#editDescription" + ctrl.id).val(), "Description:");
            ctrl.addToDiff(dmp, originalTransformation, ctrl.editor.transformation.getValue(), "Transformation:");
            ctrl.addToDiff(dmp, originalVariables, ctrl.editor.variable.getValue(), "Variables:");
            $('#modal' + ctrl.id).modal('show');
        }
    }

    ctrl.addToDiff = function (dmp, original, newText, text) {
        if (original === undefined || original === null) original = "";
        if (newText === undefined || newText === null) newText = "";
        if (text === undefined || text === null) text = "";

        if (newText !== original) {
            let diff = dmp.diff_main(original, newText);
            dmp.diff_cleanupSemantic(diff);
            ctrl.diff.push({text: text, diff: diff});
            return true;
        }
        return false;
    }

    ctrl.toggleEditCheckpoint = function () {
        console.info("Toggling edit for report");
        if (ctrl.editing) {
            let editedText = (("editor" in ctrl) ? ctrl.editor.getValue() : ctrl.reportDetails.text);
            if (editedText !== ctrl.reportDetails.text) {
                ctrl.diff = [];
                let dmp = new diff_match_patch();
                ctrl.addToDiff(dmp, ctrl.reportDetails.text, editedText, "Text:");
                $('#modal' + ctrl.id).modal('show');
            } else {
                ctrl.saveEditField(false);
            }
        } else {
            let height = Math.min((ctrl.reportDetails.text.split(/\r\n|\r|\n/).length + 1) * 15, 300);
            $('#checkpoint-monaco' + ctrl.id).css('height', height + 'px');
            ctrl.editing = true;
            require(['vs/editor/editor.main'], function () {
                ctrl.editor = monaco.editor.create(document.getElementById('checkpoint-monaco' + ctrl.id), {
                    value: ctrl.reportDetails.text,
                    language: 'xml',
                    fontSize: "10px",
                    scrollBeyondLastLine: false,
                    minimap: {
                        enabled: false
                    }
                });
            });
        }
    }

    ctrl.saveEditField = function (save) {
        console.info("Saving edit field values.");
        if (ctrl.isReportNode()) {
            let originalTransformation = "";
            if ("transformation" in ctrl.reportDetails.data && ctrl.reportDetails.data.transformation)
                originalTransformation = ctrl.reportDetails.data.transformation;
            let originalVariables = "";
            if ("variables" in ctrl.reportDetails.data && ctrl.reportDetails.data.variables)
                originalVariables = ctrl.reportDetails.data.variables;

            let updated = {};
            let data = [
                {"text": "name", "original": ctrl.reportDetails.data.name, "new": $("#editName" + ctrl.id).val()},
                {"text": "path", "original": ctrl.reportDetails.data.path, "new": $("#editPath" + ctrl.id).val()},
                {"text": "description", "original": ctrl.reportDetails.data.description, "new": $("#editDescription" + ctrl.id).val()},
                {"text": "transformation", "original": originalTransformation, "new": ctrl.editor.transformation.getValue()},
                {"text": "variables", "original": originalVariables, "new": ctrl.editor.variable.getValue()},
            ]
            for (let i = 0; i < data.length; i++) {
                if (data[i]["original"] !== data[i]["new"])
                    updated[data[i]["text"]] = data[i]["new"];
            }
            if (Object.keys(updated).length !== 0) {
                console.debug("Updating the report data with values", updated);
                $http.post("../report/" + ctrl.storage + "/" + ctrl.selectedNode["ladybug"]["storageId"], updated)
                    .then(function (response) {
                        let ladybugData = response.data.report;
                        if ("xml" in response.data) ladybugData.message = response.data.xml;
                        $rootScope.overwritten_checkpoints["report"][ladybugData.storageId] = ladybugData;

                        $('#details-edit' + ctrl.id).text("").append("<i class=\"fa fa-edit\"></i>")
                        ctrl.editing = false;
                        $('#modal' + ctrl.id).modal('hide');
                        ctrl.display_node(ctrl.selectedNode, null, null);
                    }, function (response) {
                        let title = "Update failed!";
                        if (response.status === 401) title = "Method not allowed!";
                        createToast(title, "Update of the report with storage id [" +
                            ctrl.selectedNode["ladybug"]["storageId"] + "] failed.", $scope, $compile);
                        console.error(title, response);
                    });
            }
        } else {
            ctrl.reportDetails.text = ((save && "editor" in ctrl) ? ctrl.editor.getValue() : ctrl.reportDetails.text);

            console.debug("Updating checkpoint data with", {"text": ctrl.reportDetails.text});
            $rootScope.overwritten_checkpoints["checkpoint"][ctrl.reportDetails.data.uid] = ctrl.reportDetails.text;
            ctrl.reportDetails.data["message"] = ctrl.reportDetails.text;

            $('#details-edit' + ctrl.id).text("").append("<i class=\"fa fa-edit\"></i>")
            ctrl.editing = false;
            $('#modal' + ctrl.id).modal('hide');
            ctrl.display_node(ctrl.selectedNode, null, null);
        }
    }

    ctrl.copyReport = function (to) {
        let data = {};
        let storageId = ctrl.selectedNode["ladybug"]["storageId"]
        data[ctrl.storage] = [storageId];
        console.info("Copying report with storage id [" + storageId + "] to [" + to + "]");
        $http.put("../report/store/" + to, data);
    }

    ctrl.downloadReports = function (exportReport, exportReportXml) {
        let queryString = "?id=" + ctrl.selectedNode["ladybug"]["storageId"] + "";
        window.open("../report/download/" + ctrl.storage + "/" + exportReport + "/" +
            exportReportXml + queryString);
    }

    /*
     * Displays a selected nodes in the content pane.
     */
    ctrl.display_node = function (rootNode, event, node) {
        ctrl.selectedNode = rootNode;
        let ladybugData;

        if (node === null && rootNode === null) {
            ctrl.reportDetails = {text: "", values: {}, notifications: {}};
            return;
        } else if (node === null && rootNode !== null) {
            ladybugData = ctrl.reportDetails.data;
        } else {
            ladybugData = node["ladybug"];
        }

        console.info("Displaying node");
        console.debug("Displaying", ladybugData);
        if (ctrl.isReportNode(node)) {
            // If node is report
            ctrl.display_report(ladybugData);
        } else {
            // If node is checkpoint
            ctrl.display_checkpoint(ladybugData);
        }
        if (node) ctrl.reportDetails.nodeId = node.nodeId;

        // Callback on display.
        if ('onDisplay' in ctrl.onSelectRelay && typeof ctrl.onSelectRelay.onDisplay === 'function')
            ctrl.onSelectRelay.onDisplay(ctrl.reportDetails);

        try {
            if (!$scope.$$phase) $scope.$apply();
        } catch (e) {
            console.error("Error from $scope.$apply", e);
        }
    };

    ctrl.display_checkpoint = function (ladybugData) {
        let message = (ladybugData.hasOwnProperty("message") &&  ladybugData.message !== null) ? ladybugData.message : "";
        if (ladybugData["uid"] in $rootScope.overwritten_checkpoints["checkpoint"])
            message = $rootScope.overwritten_checkpoints["checkpoint"][ladybugData["uid"]];
        ctrl.reportDetails = {
            data: ladybugData,
            text: message,
            values: {
                "Name:": ladybugData["name"],
                "Thread name:": ladybugData["threadName"],
                "Source class name:": ladybugData["sourceClassName"],
                "Checkpoint UID:": ladybugData["uid"],
                "Number of characters:": message.length,
                "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
            },
            notifications: {}
        };
        ctrl.stubStrategies = ["Follow report strategy", "No", "Yes"];
        ctrl.stubStrategySelect = ctrl.stubStrategies[ladybugData["stub"] + 1];

        $('#code' + ctrl.id).text(ctrl.reportDetails.text);
        ctrl.highlight_code();
    };

    ctrl.display_report = function (ladybugData) {
        if (ladybugData.storageId in $rootScope.overwritten_checkpoints["report"]) {
            ladybugData = $rootScope.overwritten_checkpoints.report[ladybugData.storageId];
        }
        if ("message" in ladybugData) {
            ctrl.reportDetails.text = ladybugData["message"];
            $('#code' + ctrl.id).text(ladybugData["message"]);
        } else {
            let transformer = "applyTransformer" in ctrl && ctrl["applyTransformer"];
            console.info("Getting message with URL: ../report/" + ctrl.storage + "/" + ladybugData.storageId +
                "?xml=true&globalTransformer=" + transformer);
            $http.get("../report/" + ctrl.storage + "/" + ladybugData.storageId + "?xml=true&globalTransformer=" + transformer)
                .then(function (response) {
                    console.debug("Returned message", response.data);

                    let reportXml = response.data["xml"]
                    ctrl.reportDetails.text = reportXml;
                    ladybugData.message = reportXml;
                    $('#code' + ctrl.id).text(reportXml);
                    ctrl.highlight_code();
                }, function (response) {
                    let title = "Could not get Message!";
                    if (response.status === 401) title = "Method not allowed!";
                    createToast(title, "Could not get the message from the report with storage id [" +
                        ctrl.selectedNode["ladybug"]["storageId"] + "].", $scope, $compile);
                    console.error(title, response);
                });
        }
        ctrl.reportDetails = {
            data: ladybugData,
            values: {
                "Name:": ladybugData["name"],
                "Path:": ladybugData["path"],
                "Transformation:": ladybugData["transformation"],
                "StorageId:": ladybugData["storageId"],
                "Storage:": ctrl.storage,
                "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
            },
            notifications: {}
        };
        let stubStrategySelect = ladybugData["stubStrategy"];
        ctrl.stubStrategies = $scope.testtoolStubStrategies;
        ctrl.stubStrategySelect = stubStrategySelect;

    }

    ctrl.applyCompare = function (diff, identifier) {
        ctrl.reportDetails.diff = diff;
        ctrl.reportDetails.diffIdentifier = identifier;
        ctrl.highlight_code();
    }

    ctrl.getReportText = function () {
        return ctrl.reportDetails.text;
    }

    ctrl.close_notification = function (key) {
        delete ctrl.reportDetails.notifications[key];
    }

    ctrl.escape_html = function(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    ctrl.highlight_code = function () {
        document.querySelectorAll('pre code').forEach((block) => {
            hljs.highlightBlock(block);
        });
    }

    ctrl.isReportNode = function (node) {
        if (node && "ladybug" in node) {
            ctrl.displayingReport = node["ladybug"]["stub"] === undefined;
        }
        return ctrl.displayingReport;
    }

    ctrl.$onInit = function () {
        if ("select" in ctrl.onSelectRelay) {
            let method = ctrl.onSelectRelay.select;
            ctrl.onSelectRelay.select = function (rootNode, event, node) {
                method(rootNode, event, node);
                ctrl.display_node(rootNode, event, node);
            }
        } else {
            ctrl.onSelectRelay.select = ctrl.display_node;
        }
        ctrl.onSelectRelay.getReportText = ctrl.getReportText;
        ctrl.onSelectRelay.applyCompare = ctrl.applyCompare;

        ctrl.stubStrategies = $scope.testtoolStubStrategies;

        // This will contain edited reports and checkpoint that have not been saved in the storage.
        if (! ($rootScope.overwritten_checkpoints))
            $rootScope.overwritten_checkpoints = {"report": {}, "checkpoint": {}};
    }
}

angular.module('ladybugApp').component('reportDisplay', {
    templateUrl: 'components/display/display.html',
    controller: ['$rootScope', '$scope', '$compile', '$http', displayController],
    bindings: {
        onSelectRelay: '=',
        applyTransformer: '=',
        storage: '='
    }
});