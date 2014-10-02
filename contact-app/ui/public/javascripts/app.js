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

.controller("dataController", ["$http", "$scope", "$routeParams", "$interval", 
  function($http, $scope, $routeParams, $interval) {
  
  $scope.appId = $routeParams.appId;
  $scope.extractions = [];
  $scope.urls = [];
  $scope.numSecondsElapsed = 0;
  $scope.appStatus = "Waiting" 
  var dataUrl = "/data/"+ $scope.appId;
  
  var timerPromise = $interval(function(){
    $scope.numSecondsElapsed += 1;
  }, 1000);


  var source = new EventSource(dataUrl);

  source.addEventListener("extracted", function(msg){ 
    var extraction = JSON.parse(msg.data.toString());
    //console.log($scope.appId  + ": " + extraction);
    $scope.extractions.push(extraction);
    $scope.extractions = _.uniq($scope.extractions, false, function(x) { return x.value; });
    $scope.$digest();
  }, false);

  source.addEventListener("url_processed", function(msg){ 
    $scope.appStatus = "Running";
    var url = msg.data.toString();
    //console.log($scope.appId + ": " + url);
    $scope.urls.push(url);
    $scope.$digest();
  }, false);

  source.addEventListener("terminated", function(msg){
    source.close();
    $interval.cancel(timerPromise);
    $scope.appStatus = "Finished"; 
  });

}])