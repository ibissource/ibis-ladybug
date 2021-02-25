function displayController($scope, $http) {
    let ctrl = this;
    ctrl.apiUrl = "http://localhost:8080/ibis_adapterframework_test_war_exploded/ladybug";
    ctrl.reportDetails = {text: "", values: {}};
    ctrl.stubStrategySelect = "";
    ctrl.availableStorages = {'runStorage': 'Test'};

    ctrl.rerun = function () {
        let data = {};
        data[$scope.storage] = [ctrl.selectedNode["ladybug"]["storageId"]];
        console.log("RERUN");
        console.log(data);
        $http.post(ctrl.apiUrl + "/runner/run/" + ctrl.storage, data)
            .then(function (response) {
                console.log(response);
            }, function (response) {
                console.log(response);
            });
    };

    ctrl.toggleEdit = function () {
        console.log("toggling");
        let codeWrappers = $('#code-wrapper');
        let htmlText = '<pre id="code-wrapper"><code id="code" class="xml"></code></pre>';
        let buttonText = 'Edit';

        if (codeWrappers.get().length === 0 || codeWrappers.get()[0].tagName === "PRE") {
            let rows = Math.min(20, ctrl.reportDetails.text.split('\n').length);
            htmlText = '<div id="code-wrapper" class=\"form-group\"><textarea class=\"form-control\" id=\"code\" rows="' + rows + '"></textarea></div>';
            buttonText = 'Save';
        }
        // TODO: Save the text|
        codeWrappers.remove();
        $('#details-edit').text(buttonText);
        $('#details-row').after(htmlText);
        $('#code').text(ctrl.reportDetails.text);
    }

    ctrl.copyReport = function (to) {
        let data = {};
        data[ctrl.storage] = [ctrl.selectedNode["ladybug"]["storageId"]];
        $http.put(ctrl.apiUrl + "/report/store/" + to, data);
    }

    ctrl.downloadReports = function (exportReport, exportReportXml) {
        queryString = "?id=" + ctrl.selectedNode["ladybug"]["storageId"] + "";
        window.open(ctrl.apiUrl + "/report/download/" + ctrl.storage + "/" + exportReport + "/" +
            exportReportXml + queryString);
    }

    /*
     * Displays a selected reports in the content pane.
     */
    ctrl.display_report = function (rootNode, event, node) {
        ctrl.selectedNode = rootNode;
        if (node === null) {
            ctrl.reportDetails = {text: "", values: {}};
            return;
        }
        let ladybugData = node["ladybug"];
        $('#code-wrapper').remove();
        $('#details-row').after('<pre id="code-wrapper"><code id="code" class="xml"></code></pre>');

        if (ladybugData["stubStrategy"] === undefined) {
            // If node is checkpoint
            ctrl.display_checkpoint(ladybugData);
        } else {
            // If node is report
            ctrl.display_report_(ladybugData);
        }
        ctrl.reportDetails.nodeId = node.nodeId;
        if (!$scope.$$phase) $scope.$apply();
    };

    ctrl.display_checkpoint = function (ladybugData) {
        console.log("ladybugData as checkpoint", ladybugData);
        $('#code').text(ladybugData["message"]);
        ctrl.reportDetails = {
            data: ladybugData,
            text: ladybugData["message"],
            values: {
                "Name:": ladybugData["name"],
                "Thread name:": ladybugData["threadName"],
                "Source class name:": ladybugData["sourceClassName"],
                "Path:": "???",
                "Checkpoint UID:": ladybugData["uid"],
                "Number of characters:": "???",
                "EstimatedMemoryUsage:": ladybugData["estimatedMemoryUsage"]
            },
        };
        let stubStrategySelect = ladybugData["stubStrategy"];
        stubStrategySelect = ladybugData["stub"];
        ctrl.stubStrategies = ["Follow report strategy", "No", "Yes"];
        ctrl.stubStrategySelect = ctrl.stubStrategies[stubStrategySelect + 1];
        ctrl.highlight_code();
    };

    ctrl.display_report_ = function (ladybugData) {
        console.log("ladybugData as report", ladybugData);
        if ("message" in ladybugData) {
            ctrl.reportDetails.text = ladybugData["message"];
            $('#code').text(ladybugData["message"]);
        } else {
            console.log("URL:" + ctrl.apiUrl + "/report/" + ctrl.storage + "/" + ladybugData.storageId + "?xml=true");
            $http.get(ctrl.apiUrl + "/report/" + ctrl.storage + "/" + ladybugData.storageId + "?xml=true")
                .then(function (response) {
                    console.log("Message", response.data);
                    // let reportXml = ctrl.escape_html(response.data["xml"]);
                    let reportXml = response.data["xml"]
                    console.log("XML DATA", reportXml);
                    ctrl.reportDetails.text = reportXml;
                    ladybugData["message"] = reportXml;
                    $('#code').text(reportXml);
                    ctrl.highlight_code();
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
        };
        let stubStrategySelect = ladybugData["stubStrategy"];
        ctrl.stubStrategies = $scope.testtoolStubStrategies;
        ctrl.stubStrategySelect = stubStrategySelect;

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
        console.log("Highlightin!!");
        document.querySelectorAll('pre code').forEach((block) => {
            console.log("LOL");
            hljs.highlightBlock(block);
        });
    }

    ctrl.$onInit = function () {
        if ("select" in ctrl.onSelectRelay) {
            let method = ctrl.onSelectRelay.select;
            ctrl.onSelectRelay.select = function (rootNode, event, node) {
                method(rootNode, event, node);
                ctrl.display_report(rootNode, event, node);
            }
        } else {
            ctrl.onSelectRelay.select = ctrl.display_report;
        }

        ctrl.stubStrategies = $scope.testtoolStubStrategies;
    }
}

angular.module('myApp').component('reportDisplay', {
    templateUrl: 'components/display/display.html',
    controller: ['$scope', '$http', displayController],
    bindings: {
        onSelectRelay: '=',
        storage: '='
    }
});