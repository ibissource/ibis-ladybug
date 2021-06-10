'use strict';

// Declare app level module which depends on views, and core components
angular.module('ladybugApp', [
  'ngRoute',
  'ladybugApp.view1',
  'ladybugApp.view2',
  'ladybugApp.view3',
  'ladybugApp.report'
]).
config(['$locationProvider', '$routeProvider', function($locationProvider, $routeProvider) {
  $locationProvider.hashPrefix('!');

  $routeProvider.otherwise({redirectTo: '/view1'});
}]);
