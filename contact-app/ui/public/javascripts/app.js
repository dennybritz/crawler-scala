"use strict";

angular.module("blikk-contactapp", ["ngRoute"])

.config(["$routeProvider", function($routeProvider) {
  $routeProvider.when("/start", {
    templateUrl: "/start",
    controller: "startController"
  })
  .when("/data/:appId", {
    templateUrl: "/data",
    controller: "dataController"
  })
  .otherwise({redirectTo: "/start"});
}])

.controller("startController", ["$scope", "$http", "$location", function($scope, $http, $location) {
  
  $scope.startApp = function(url){
    $http.post("/start", {url: url}).success(function(appId){
      console.log(appId);
      $location.path("/data/" + appId);
    });
  }

}])

.controller("dataController", ["$http", "$scope", "$routeParams", function($http, $scope, $routeParams) {
  
  $scope.appId = $routeParams.appId;
  $scope.extractions = [];
  $scope.urls = [];
  var dataUrl = "/data/"+ $scope.appId;
  
  var source = new EventSource(dataUrl);
  source.addEventListener("extracted", function(msg){ 
    console.log(msg.data);
    var extraction = JSON.parse(msg.data.toString());
    console.log(extraction);
    $scope.extractions.push(extraction);
    $scope.extractions = _.uniq($scope.extractions, false, function(x) { return x.value; });
    $scope.$digest();
  }, false);

  source.addEventListener("url_processed", function(msg){ 
    var url = msg.data;
    console.log(url);
    $scope.urls.push(url);
    $scope.$digest();
  }, false);

}])