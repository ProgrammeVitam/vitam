/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * 
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and
 * efficiently.
 * 
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL 2.1 license
 * as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

// Define controller for search operation
angular
		.module('search.operation')
		.controller(
				'searchOperationController',
				function($scope, $http, $timeout, $window, lodash,
						searchOperationService, ihmDemoCLient,
						downloadOperationService) {

					var ctrl = this;
					ctrl.itemsPerPage = 50;
					ctrl.currentPage = 0;
					ctrl.searchOptions = {};
					ctrl.operationList = [];
					ctrl.resultPages = 0;
					ctrl.downloadOptions = {};

					function displayError(message) {
						ctrl.fileNotFoundError = true;
						ctrl.errorMessage = message;
						// ctrl.timer = $timeout(function() {
						// ctrl.fileNotFoundError = false;
						// }, 5000);
					}
					ctrl.goToDetails = function(id) {
						$window
								.open('#!/searchOperation/detailOperation/'
										+ id)
					};

					ctrl.getList = function() {
						ctrl.operationList = [];
						ctrl.fileNotFoundError = false;

						ctrl.searchOptions.EventType = 'traceability';
						ctrl.searchOptions.EvDateTime = $scope.startDate;

						if (ctrl.searchOptions.EvDateTime == ""
								|| ctrl.searchOptions.EvDateTime == undefined) {
							displayError("Veuillez choisir une date");
						}

						ctrl.searchOptions.orderby = "evDateTime";
						// ctrl.client.all('operations').post(ctrl.searchOptions).then(function(response)
						searchOperationService
								.getOperations(
										ctrl.searchOptions,
										function(response) {
											ctrl.operationList = response.data.$results;
											if (ctrl.operationList.length === 0) {
												ctrl.results = 0;
												displayError("Il n'y a aucun résultat pour votre recherche");
												return;
											}
											ctrl.resultPages = Math
													.ceil(ctrl.operationList.length
															/ ctrl.itemsPerPage);
											ctrl.currentPage = 1;
											ctrl.results = response.data.$hits.total;
										},
										function() {
											ctrl.searchOptions = {};
											ctrl.resultPages = 0;
											ctrl.currentPage = 0;
											ctrl.results = 0;
											if (ctrl.searchDate) {
												displayError("Veuillez choisir une date");
											} else {
												displayError("Il n'y a aucun résultat pour votre recherche");
											}

										});
					};
					ctrl.downloadOperation = function(objectId) {
						$window.open(downloadOperationService.getLogbook(objectId));
					};


				}).constant('lodash', window._);
