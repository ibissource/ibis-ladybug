'use strict';

angular.module('ladybugApp.view1', ['ngRoute'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/view1', {
            templateUrl: 'views/debug/view1.html',
            controller: 'View1Ctrl'
        });
    }])

    .controller('View1Ctrl', ['$scope', function ($scope) {
        $scope.storage = "debugStorage";
        $scope.table2tree = {};
        $scope.tree2display = {};
    }]);