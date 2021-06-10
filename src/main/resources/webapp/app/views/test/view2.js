'use strict';

angular.module('ladybugApp.view2', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view2', {
            templateUrl: 'views/test/view2.html',
            controller: 'View2Ctrl'
        });
    }])

    .controller('View2Ctrl', ['$scope', '$http', '$compile', function ($scope, $http, $compile) {
        $scope.storage = 'testStorage';
        $scope.debugStorage = 'debugStorage';
        $scope.moveTo = "/";
        $scope.showOptions = {ids: false, checkpoints: false};
        $scope.reports = [];
        $scope.treeData = [{}];
        $scope.cloneInputs = {csv: "", message: "", storageId: "", force: false};
        $scope.reranReports = {};
        $scope.progressBar = {max: -1, progress: -1};

        $scope.addNode = function (rootNode, path, data) {
            console.debug("Adding node with data", {path: path, data: data, rootnode: rootNode});
            if (path === undefined || path.length === 0 || (path.length === 1 && path[0] === "")) {
                rootNode["ladybug"].push(data);
                return;
            }

            let nodes = rootNode["nodes"];
            for (let i = 0; i < nodes.length; i++) {
                if (nodes[i]["path"] === path[0]) {
                    $scope.addNode(nodes[i], path.slice(1), data);
                    return;
                }
            }
            let node = {path: path[0], text: path[0], icon: "fa fa-file-code", ladybug: [], nodes: []};
            nodes.push(node);
            $scope.addNode(node, path.slice(1), data);
        };

        $scope.openNode = function (node, checked) {
            let reports = node["ladybug"];
            for (let i = 0; i < reports.length; i++) {
                reports[i]["checked"] = checked;
                reports[i]["csvText"] = $scope.csv2text(reports[i]["variableCsv"]);
                $scope.reports.push(reports[i]);
            }
            let nodes = node["nodes"];
            for (let i = 0; i < nodes.length; i++) {
                $scope.openNode(nodes[i], false);
            }
        };

        $scope.getSelectedReports = function () {
            let reports = [];
            for (let i = 0; i < $scope.reports.length; i++) {
                if ($scope.reports[i]["checked"] === true)
                    reports.push($scope.reports[i]);
            }
            return reports;
        }

        $scope.openReport = function (reports) {
            window.location = "#!/report?storage=" + $scope.storage + "&storageId=" + reports[0]["storageId"];
        };

        $scope.runReport = function () {
            let reports = $scope.getSelectedReports();
            let data = {};
            data[$scope.storage] = [];
            for (let i = 0; i < reports.length; i++) {
                data[$scope.storage].push(reports[i]["storageId"]);
            }
            console.info("Running reports", data);
            $http.post("../runner/run/" + $scope.debugStorage, data)
                .then(function (response) {
                    console.debug("Run report response:", response);
                    setTimeout($scope.queryResults, 100); //wait 0.1 seconds
                }, function (response) {
                    let title = "Error while running!";
                    if (response.status === 401) title = "Method not allowed!";
                    let storageIds = "";
                    for (let i = 0; i < reports.length; i++) {
                        storageIds = storageIds + reports[i]["storageId"] + ", ";
                    }
                    createToast(title,
                        "Error while running the reports [" + storageIds.slice(0, storageIds.length - 2) + "]",
                        $scope, $compile);
                    console.error(title, response);
                })
        };

        $scope.queryResults = function () {
            $http.get("../runner/result/" + $scope.debugStorage).then(function (response) {
                for (const [key, value] of Object.entries(response.data.results)) {
                    $scope.reranReports[key] = {
                        text: "(" + value["current-time"] + " >> " + value["current-time"] + " ms) " +
                            "(" + value["stubbed"] + "/" + value["total"] + " stubbed)",
                        report: value["report"]
                    };
                }
                $scope.progressBar = {max: 100, progress: response.data['progress'] * 100 / response.data['max-progress']};
                if (response.data['progress'] !== response.data['max-progress']) {
                    setTimeout($scope.queryResults, 100); //wait 0.1 seconds
                }
            }, function (response) {
                let title = "Error in polling!";
                if (response.status === 401) title = "Method not allowed!";
                createToast(title, "Could not query the results. Check logs for more information.", $scope, $compile);
                console.error(title, response);
            });
        }

        $scope.treeSelect = function () {
            let nodes = $('#tree').treeview('getSelected');
            $scope.reports = [];
            for (let i = 0; i < nodes.length; i++) {
                $scope.openNode(nodes[i], true);
            }
            if (!$scope.$$phase) $scope.$apply();
        };

        $scope.updateTree = function () {
            console.info("Updating tree on test tab.");
            $http.get("../metadata/" + $scope.storage + "?path=&storageId=&status=")
                .then(function (response) {
                    let fields = response.data["fields"];
                    let values = response.data["values"];
                    let rootNode = {
                        path: "/",
                        text: "Reports",
                        icon: "fa fa-file-code",
                        ladybug: [],
                        nodes: [],
                        state: {selected: true}
                    };
                    for (let i = 0; i < values.length; i++) {
                        let data = {};
                        for (let v = 0; v < fields.length; v++) {
                            data[fields[v]] = values[i][v];
                        }

                        let path = data["path"];
                        if (path.startsWith("/"))
                            path = path.slice(1);
                        if (path.endsWith("/"))
                            path = path.slice(0, -1);

                        $scope.addNode(rootNode, path.split("/"), data);
                    }

                    $scope.treeData = [rootNode];
                    $('#tree').treeview({
                        data: $scope.treeData,
                        onNodeSelected: $scope.treeSelect
                    });
                    $scope.treeSelect();
                }, function (response) {
                    let title = "Error!";
                    if (response.status === 401) title = "Method not allowed!";
                    createToast(title, "Error while getting tree metadata.", $scope, $compile);
                    console.error(title, response);
                });
        };

        $scope.replaceReport = function (report) {
            $http.put("../runner/replace/" + $scope.debugStorage + "/" + report.storageId)
                .then(function (response) {
                    delete $scope.reranReports[report.storageId];
                }, function (response) {
                    let title = "Replace Error!";
                    if (response.status === 401) title = "Method not allowed!";
                    createToast(title, "Error while replacing report with id [" + report.storageId + "]. Check logs for more information.", $scope, $compile);
                    console.error(title, response);
                });
        };

        $scope.uploadReport = function () {
            let files = document.getElementById("upload-file").files;
            if (files.length === 0)
                return;
            for (let i = 0; i < files.length; i++) {
                console.debug("Uploading report from file with name [%s]", files[i].name);

                let formdata = new FormData();
                formdata.append("file", files[i]);
                $http.post("../report/upload/" + $scope.storage, formdata, {headers: {"Content-Type": undefined}});
            }
        }

        $scope.deleteReports = function () {
            let reports = $scope.getSelectedReports();
            for (let i = 0; i < reports.length; i++) {
                console.debug("Deleting report with storage id [" + reports["storageId"] + "]");
                $http.delete("../report/" + $scope.storage + "/" + reports["storageId"]);
            }
        }

        $scope.downloadReports = function (reports, exportReport, exportReportXml) {
            let queryString = "?";
            for (let i = 0; i < reports.length; i++) {
                queryString += "id=" + reports[i]["storageId"] + "&";
            }

            console.info("Downloading reports with query", queryString);
            window.open("../report/download/" + $scope.storage +
                "/" + exportReport + "/" + exportReportXml + queryString.slice(0, -1));
        }

        $scope.moveReports = function (reports, action) {
            let path = $('#moveToInput').val();
            for (let i = 0; i < reports.length; i++) {
                console.info("Running action [%s] on report with storage id [%d] with path [%s]", action, reports[i]["storageId"], path);
                $http.put("../report/move/" + $scope.storage + "/" + reports[i]["storageId"],
                    {path: path, action: action}).then(function (response) {
                    $scope.refresh();
                });
            }
        };

        $scope.openCloneModal = function (reports) {
            let storageId = reports[0]["storageId"]
            $http.get("../report/" + $scope.storage + "/" + storageId)
                .then(function (response) {
                    $scope.cloneInputs.message = response.data["inputCheckpoint"]["message"];
                    $scope.cloneInputs.storageId = storageId;
                    $('#cloneModal').modal('show');
                }, function (response) {
                    let title = "Error while getting Report!";
                    if (response.status === 401) title = "Method not allowed!";
                    createToast(title, "Could not get the report with storage id [" + storageId + "]", $scope, $compile);
                    console.error(title, response);
                })
        }

        $scope.closeCloneModal = function () {
            $scope.cloneInputs = {csv: "", message: "", storageId: "", force: false};
            $scope.refresh();
        }

        $scope.cloneReport = function () {
            let data = {
                csv: $scope.cloneInputs.csv,
                message: $scope.cloneInputs.message,
                force: $scope.cloneInputs.force
            };
            console.info("Cloning report with storage id [" + $scope.cloneInputs.storageId + "] with data", data);
            $http.post("../report/clone/" + $scope.storage + "/" + $scope.cloneInputs.storageId, data)
                .then($scope.closeCloneModal, function (response) {
                    $scope.cloneInputs.force = true;
                    let title = "Could not clone!";
                    if (response.status === 401) title = "Method not allowed!";
                    createToast(title,
                        "Could not clone the report with storage id [" + $scope.cloneInputs.storageId + "]",
                        $scope, $compile);
                    console.error(title, response);
                });
        }

        $scope.csv2text = function (csv) {
            if (csv === undefined || csv === "")
                return "";

            let lines = csv.split("\n");
            if (lines.length !== 2)
                return "";

            let keys = lines[0].split(";");
            let values = lines[1].split(";");
            if (keys.length !== values.length)
                return "";
            let str = "";
            for (let i = 0; i < keys.length; i++) {
                str += ", " + keys[i].trim() + " = " + values[i].trim();
            }
            return str.slice(2);
        }

        $scope.resetRunner = function () {
            $http.post("../runner/reset");
        }

        $scope.changeSelectedAll = function (value) {
            for (let i = 0; i < $scope.reports.length; i++) {
                $scope.reports[i]["checked"] = value;
            }
        };

        $scope.refresh = function () {
            $scope.updateTree();
        }

        $('#cloneModal').on('hidden.bs.modal', $scope.closeCloneModal)
        $scope.refresh();
    }]);