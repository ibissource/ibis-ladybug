'use strict';

angular.module('ladybugApp.report', ['ngRoute'])

.config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/report', {
        templateUrl: 'views/report/report.html',
        controller: 'ReportCtrl'
    });
}])

.controller('ReportCtrl', ['$scope', '$rootScope', '$location', '$http', function ($scope, $rootScope, $location, $http) {
    $rootScope.openedReports = [];
    $rootScope.tabs = ($rootScope.hasOwnProperty("tabs")) ? $rootScope.tabs : {};
    $scope.addRelay = {};
    $scope.tree2display = {};
    $scope.storage = 'testStorage';
    $scope.table2tree = {transformationEnabled: true};

    console.log("Report Controller Started");

    $scope.delete_tab = function (storageId) {
        console.log("Jamiryooooo");
        $("#" + storageId).remove();
        window.location = "#!/view1";
    }

    $scope.add_tab = function (tabName, storageId, storage) {
        if (!$('#' + storageId).length) {
            $rootScope.tabs[storageId] = tabName;
            let close = "<i class=\"fa fa-times\" aria-hidden=\"true\" " +
                "onclick=\"$('#" + storageId + "').remove();window.location = '#!/view1';\"></i>";

            $('#ladybug-tabs').append(
                "<li class=\"nav-item\" id=\"" + storageId + "\">" +
                "<a href=\"#!/report?storage=" + storage +  "&storageId=" + storageId + "\" " +  "data-toggle=\"tab\" class=\"nav-link\">" +
                tabName + "</a></li>");

            $('#' + storageId + " .nav-link")[0].click()
        }
    }

    $scope.addRelay.postInit = function() {
        console.log("Report Controller oninit");
        let searchObject = $location.search();
        console.log("openreport", searchObject);

        $http.get("../report/" + searchObject.storage + "/" + searchObject.storageId)
            .then(function (response) {
                // TODO: get updated info from rootscope
                console.log("Open Report on init");
                console.log(response.data);
                $scope.add_tab(response.data.name, response.data.storageId, searchObject.storage);
                $scope.addRelay.add(response.data);
                window.setTimeout(() => {
                    $scope.tree2display.selectPath([0]);
                }, 500);
            }, function (response) {
                console.error(response);
            });
    }
}]);