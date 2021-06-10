'use strict';
// TODO: Create a list of features for documentation.

angular.module('ladybugApp.view3', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/view3', {
    templateUrl: 'views/compare/view3.html',
    controller: 'View3Ctrl'
  });
}])

.controller('View3Ctrl', ['$scope', function($scope) {
  $scope.texts = {};
  let callback = function (side) {
    return function (reportDetails) {
      $scope.texts[side] = reportDetails.text;
      if (Object.keys($scope.texts).length === 2)
        $scope.calculateDiff();
    }
  }

  $scope.storage = "debugStorage";
  $scope.left_table2tree = {};
  $scope.left_tree2display = {onDisplay: callback("left")};
  $scope.right_table2tree = {};
  $scope.right_tree2display = {onDisplay: callback("right")};
  $scope.left_show = true;
  $scope.right_show = true;

  $scope.left_tree2display.select = function (rootNode, event, node) {
    $scope.texts = {};
    $scope.left_show = node === null;
    let path = $scope.left_tree2display.getPath(node);
    $scope.right_tree2display.selectPath(path);
    $scope.$apply();
  };

  $scope.right_tree2display.select = function (rootNode, event, node) {
    $scope.texts = {};
    $scope.right_show = node === null;
    let path = $scope.right_tree2display.getPath(node);
    $scope.left_tree2display.selectPath(path);
    $scope.$apply();
  };

  $scope.calculateDiff = function () {
    // Run diff on line mode
    let dmp = new diff_match_patch();
    let charDiff = dmp.diff_linesToChars_($scope.texts["left"], $scope.texts["right"]);
    let lineText1 = charDiff.chars1;
    let lineText2 = charDiff.chars2;
    let lineArray = charDiff.lineArray;
    let diffs = dmp.diff_main(lineText1, lineText2, false);
    dmp.diff_charsToLines_(diffs, lineArray);
    dmp.diff_cleanupSemantic(diffs);

    $scope.left_tree2display.applyCompare(diffs, -1);
    $scope.right_tree2display.applyCompare(diffs, 1);
  }
}]);