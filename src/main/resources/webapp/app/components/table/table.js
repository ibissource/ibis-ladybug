'use strict';

function metadataTableController($scope, $compile, $http) {
    var ctrl = this;
    ctrl.columns = ['storageId', 'endTime', 'duration', 'name', 'correlationId', 'status', 'nrChpts', 'estMemUsage', 'storageSize'];
    ctrl.storage = "debugStorage";
    ctrl.limit = 5;
    ctrl.filters = {"limit": ctrl.limit, "sort": "", "order": "ascending"};
    ctrl.metadatas = [];
    ctrl.options = {
        "generatorEnabled": true,
        "regexFilter": "^(?!Pipeline WebControl).*",
        "estMemory": 0,
        "reportsInProgress": 0,
        "transformation": "",
        "transformationEnabled": true,
        "openLatest": 10,
        "openInProgress": 1
    };

    /*
    * Sends a GET request to the server and updates the tables (ctrl.metadatas)
    */
    ctrl.updateTable = function () {
        console.info("Updating metadata table with filters", ctrl.filters);
        $http.get("../metadata/" + ctrl.storage, {params: ctrl.filters}).then(function (response) {
            let fields = response.data["fields"];
            let values = response.data["values"];
            ctrl.metadatas = [];
            values.forEach(function (element) {
                if (fields.length !== element.length) {
                    return;
                }
                let row = {};
                for (let i = 0; i < fields.length; i++) {
                    row[fields[i]] = element[i];
                }
                ctrl.metadatas.push(row);
            });
        }, function (response) {
            let title = "Could not update table!";
            if (response.status === 401) title = "Method not allowed!";
            createToast(title, "Error while getting metadata.", $scope, $compile)
            console.error(title, response);
        });
    };

    ctrl.downloadReports = function (exportReport, exportReportXml) {
        let queryString = "?";
        for (let i = 0; i < ctrl.metadatas.length; i++) {
            queryString += "id=" + ctrl.metadatas[i]["storageId"] + "&";
        }

        console.info("Downloading reports with query [" + queryString + "]");
        window.open("../report/download/" + ctrl.storage +
            "/" + exportReport + "/" + exportReportXml + queryString.slice(0, -1));
    }

    /*
     * Refreshes the content of the table, including getting new columns
     */
    ctrl.refresh = function () {
        console.info("Refreshing metadata.");
        $http.get('../metadata').then(function (response) {
            ctrl.columns = response.data;
            ctrl.columns.forEach(function (element) {
                if (ctrl.filters[element] === undefined) {
                    ctrl.filters[element] = "";
                }
            });
            ctrl.updateTable();
        }, function (response) {
            let title = "Error on refresh!";
            if (response.status === 401) title = "Method not allowed!";
            createToast(title, "Error while getting metadata columns.", $scope, $compile);
            console.error(title, response);
        });
    };

    ctrl.uploadReport = function() {
        let files = document.getElementById("upload-file").files;
        if (files.length === 0)
            return;
        for (let i = 0; i < files.length; i++) {
            console.info("Uploading report from file", files[i].name);

            let formdata = new FormData();
            formdata.append("file", files[i]);
            $http.post("../report/upload/", formdata, {headers: {"Content-Type": undefined}})
                .then(function (response) {
                    for (let i = 0; i < response.data.length; i++) {
                        ctrl.onSelectRelay.add(response.data[i]);
                    }
                }, function (response) {
                    let title = "Error on upload!";
                    if (response.status === 401) title = "Method not allowed!";
                    createToast(title, "Error while uploading the report [" + files[i].name + "]",
                        $scope, $compile);
                    console.error(title, response);
                });
        }
    }


    ctrl.updateFilter = function (key, value) {
        ctrl.filters[key] = value;
        ctrl.updateTable();
    }

    ctrl.updateOrder = function (column) {
        ctrl.filters["sort"] = column;
        if (ctrl.filters["order"] === "descending") {
            ctrl.filters["order"] = "ascending";
        } else {
            ctrl.filters["order"] = "descending";
        }
        ctrl.updateTable();
    }

    ctrl.selectReport = function (metadata) {
        console.info("Report selected", metadata);
        $http.get("../report/" + ctrl.storage + "/" + metadata["storageId"])
            .then(function (response) {
                console.debug("Opening report", response.data);
                ctrl.onSelectRelay.add(response.data);
            }, function (response) {
                let title = "Error on report!";
                if (response.status === 401) title = "Method not allowed!";
                createToast(title, "Could not get the report with storage id [" + metadata["storageId"] + "]",
                    $scope, $compile);
                console.error(title, response);
            });
    };

    ctrl.openAll = function () {
        ctrl.metadatas.forEach(function (element) {
            ctrl.selectReport(element);
        });
    };

    ctrl.updateOptions = function() {
        console.info("Updating options");
        $http.get("../testtool")
            .then(function (response) {
                ctrl.options = Object.assign(ctrl.options, response.data);
                ctrl.testtoolStubStrategies = response.data["stubStrategies"];
                ctrl.stubStrategies = ctrl.testtoolStubStrategies;
                console.debug("Updated options", ctrl.options);
            }, function (response) {
                let title = "Could not access Ladybug Api!";
                if (response.status === 401) title = "Method not allowed!";
                createToast(title, "Can not retrieve testtool information.", $scope, $compile);
                console.error(title, response);
            });
        console.info("Updating transformations");
        $http.get("../testtool/transformation")
            .then(function (response) {
                ctrl.options = Object.assign(ctrl.options, response.data);
                ctrl.options['transformationEnabled'] = ctrl.options['transformation'] !== "";
                console.debug("Updated transformation", {transformation: ctrl.options['transformation']});
            });
    }

    ctrl.saveOptions = function() {
        console.info("Saving Options", ctrl.options);
        $http.post("../testtool", {
            "generatorEnabled": ctrl.options['generatorEnabled'],
            "regexFilter": ctrl.options["regexFilter"]});
        $http.post("../testtool/transformation", {"transformation": ctrl.options['transformation']});
    }

    ctrl.openLatestReports = function (number) {
        console.info("Opening latest" + number + "reports");
        $http.get("../report/latest/" + ctrl.storage + "/" + number)
            .then(function (response) {
                response.data.forEach(function (report) {
                    ctrl.onSelectRelay.add(report);
                    console.debug("Opening latest report:", report);
                });
            });
    }

    ctrl.$onInit = function () {
        ctrl.updateOptions();
        ctrl.refresh();
        ctrl.onSelectRelay.transformationEnabled = ctrl.options.transformationEnabled;
    }
}

angular.module('ladybugApp').component('metadataTable', {
    templateUrl: 'components/table/table.html',
    controller: ['$scope', '$compile', '$http', metadataTableController],
    bindings: {
        onSelectRelay: '='
    }
});