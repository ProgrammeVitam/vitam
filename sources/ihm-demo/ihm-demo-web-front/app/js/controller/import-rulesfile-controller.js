/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

'use strict';

angular.module('ihm.demo')
.controller('rulesController', function($scope, FileUploader, $mdDialog, $route, authVitamService){
	$scope.mustShow = false;

	var serviceURI = "/ihm-demo/v1/api/rules";
	var checkRules = "/check";
	var uploadRules = "/upload";

	var uploader = $scope.uploader = new FileUploader({
    url : serviceURI + checkRules,
    headers: {
      'content-type': 'application/octet-stream',
      'accept' : 'application/json',
      'X-Tenant-Id': authVitamService.cookieValue(authVitamService.COOKIE_TENANT_ID)
    },
    disableMultipart: true
  });

    // FILTERS
    uploader.filters.push({
        name: 'customFilter',
        fn: function(item, options) {
            return this.queue.length < 10;
        }
    });

    uploader.onSuccessItem = function(fileItem, response, status, headers) {
    	console.info('onSuccessItem', fileItem, response, status, headers);
    	if (uploader.queue[0].url == serviceURI + checkRules){
    		var confirm = $mdDialog.confirm()
    			.title('Fichier valide')
    			.ok('Lancer l\'import')
    			.cancel('Annuler l\'import');
    		$mdDialog.show(confirm).then(uploadAction, cancelAction);
    	} else if (uploader.queue[0].url == serviceURI + uploadRules) {
    		var alert = $mdDialog.alert()
    			.title(' Le référentiel des règles de gestion est importé')
    			.ok("Fermer");
    		$mdDialog.show(alert);
    	}

    };

    uploader.onErrorItem = function(fileItem, response, status, headers) {
    	console.info('onErrorItem', fileItem, response, status, headers);
    	if (uploader.queue[0].url == serviceURI + checkRules){
    		var alert = $mdDialog.alert()
				.title('Fichier invalide ou référentiel des règles de gestion déjà existant')
				.ok("Fermer");
    		$mdDialog.show(alert).then(function(){
    			$route.reload();
    		});
    	} else if (uploader.queue[0].url == serviceURI + uploadRules) {
    	    if (status === 403) {
                var alert = $mdDialog.alert()
                                .title("Un import de règles est déjà en cours, veuillez attendre qu'il se termine.")
                                .ok("Fermer");
    	    } else {
                var alert = $mdDialog.alert()
                    .title("Echec de l'import du référentiel des règles de gestion.")
                    .ok("Fermer");
			}
    		$mdDialog.show(alert)
    	}

    };


	$scope.validAction = function(){
		uploader = $scope.uploader;
		uploader.queue[0].url = serviceURI + checkRules;
		uploader.queue[0].headers['X-Filename'] = uploader.queue[0]._file.name;
		uploader.queue[0].upload();
	};

	function uploadAction() {
		uploader = $scope.uploader;
		uploader.queue[0].url = serviceURI + uploadRules;
		uploader.queue[0].headers['X-Filename'] = uploader.queue[0]._file.name;
		uploader.queue[0].upload();

		$scope.checked = true;
	}

    function cancelAction() {
    	console.log('Canceled');
    	$route.reload();
    }
});
