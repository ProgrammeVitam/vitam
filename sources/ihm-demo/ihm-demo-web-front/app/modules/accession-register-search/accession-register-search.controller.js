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

angular.module('accession.register.search')
  .constant('ACCESSIONREGISTER_CONSTANTS', {
    'GET_ALL_REGISTERS': 'ACCESSIONREGISTER',
    'ORIGINATING_AGENCY_FIELD': 'OriginatingAgency'
  })
  .controller('accessionRegisterSearchController', function($scope, $window, ACCESSIONREGISTER_CONSTANTS, ihmDemoFactory, responseValidator,ITEM_PER_PAGE, processSearchService) {
    var self = this;

    // ************************************Pagination  **************************** //
    self.currentPage = 1;
    self.itemsPerPage = ITEM_PER_PAGE;
    self.maxSize = 5;
    self.resultPages ="";

    // FIXME Useless function ?
    self.setPage = function(pageNo) {
      selfcurrentPage = pageNo;
    };

    // FIXME Useless function ?
    self.setItemsPerPage = function(num) {
      self.itemsPerPage = num;
      self.currentPage = 1; // reset to first page
    };
    // **************************************************************************** //

    self.startFormat = function(){
      var start="";

      if(self.currentPage > 0 && self.currentPage <= self.resultPages){
        start= (self.currentPage-1)*self.itemsPerPage;
      }

      if(self.currentPage>self.resultPages){
        start= (self.resultPages-1)*self.itemsPerPage;
      }
      return start;
    };

    self.goToDetails = function(id) {
      $window.open('#!/accessionRegister/detail/' + id)
    };

    var preSearch = function(serviceProducerCriteria) {
      self.searchCriteria = {};
      self.searchCriteria.orderby = ACCESSIONREGISTER_CONSTANTS.ORIGINATING_AGENCY_FIELD;

      if(serviceProducerCriteria === null || serviceProducerCriteria === undefined
        || serviceProducerCriteria === ''){
        // Return all registers
        self.searchCriteria[ACCESSIONREGISTER_CONSTANTS.GET_ALL_REGISTERS] = ACCESSIONREGISTER_CONSTANTS.GET_ALL_REGISTERS;
      } else {
        self.searchCriteria[ACCESSIONREGISTER_CONSTANTS.ORIGINATING_AGENCY_FIELD] = serviceProducerCriteria;
      }
      return self.searchCriteria;
    };

    var successCallback = function(response) {
      var isReponseValid = responseValidator.validateReceivedResponse(response);
      if (!isReponseValid) {
        return false;
      }
      // Get total results
      self.totalResult = response.data.$hits.total;
      self.showResult = true;
      self.resultPages = Math.ceil(self.totalResult/self.itemsPerPage);

      if (self.totalResult > 0) {
        // Display found registers
        self.registers = response.data.$results;
      }
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var clearResults = function() {
      self.registers = [];
      self.currentPage = "";
      self.resultPages = "";
      self.totalResult = 0;
    };

    $scope.search = {
      form: {
      }, pagination: {
        currentPage: 0,
        resultPages: 0
      }, error: {
        message: '',
        displayMessage: false
      }, response: {
        data: [],
        hints: {},
        totalResult: 0
      }
    };

    self.searchRegistersByCriteria =  processSearchService.initAndServe(ihmDemoFactory.getAccessionRegisters, preSearch, successCallback, computeErrorMessage, $scope.search, clearResults, true, null);

  });

