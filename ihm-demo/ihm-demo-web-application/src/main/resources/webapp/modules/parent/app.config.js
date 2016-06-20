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
	      when('/form/:archiveId', {
	    	  template: '<form-archive></form-archive>'
	      }).
	      otherwise('/uploadSIP');
    }
  ]);
