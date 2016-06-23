'use strict';

angular.
  module('ihm.demo').
  config(['$locationProvider' ,'$routeProvider',
    function config($locationProvider, $routeProvider) {
      $locationProvider.hashPrefix('!');

      $routeProvider.
	      when('/archiveSearch', {
	    	  template: '<archive-search></archive-search>'
	      }).
	      when('/admin', {
	          templateUrl: "views/admin-log.html",
	          controller: "logbookController as logctrl"
	      }).
	      when('/uploadSIP', {
	          templateUrl: "views/upload-sip.html",
	          controller: "uploadController"
	      }).
	      when('/archiveunit/:archiveId', {
	    	  template: '<archive-unit></archive-unit>'
	      }).
	      otherwise('/uploadSIP');
    }
  ]);
