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
  .controller('accessionRegisterSearchController', function(ACCESSIONREGISTER_CONSTANTS, ihmDemoFactory, responseValidator) {
    var self = this;

    // ************************************Pagination  **************************** //
    self.viewby = 10;
    self.currentPage = 1;
    self.itemsPerPage = self.viewby;
    self.maxSize = 5;

    self.setPage = function(pageNo) {
     selfcurrentPage = pageNo;
    };

    self.setItemsPerPage = function(num) {
      self.itemsPerPage = num;
      self.currentPage = 1; // reset to first page
    };
    // **************************************************************************** //

    // Search criteria
    // Default criteria
    // Search for accession register
    self.searchRegistersByCriteria = function(serviceProducerCriteria) {
      self.searchCriteria = {};
      self.searchCriteria.orderby = 'creationDate';

      if(serviceProducerCriteria === null || serviceProducerCriteria === undefined
        || serviceProducerCriteria === ''){
        // Return all registers
        self.searchCriteria[ACCESSIONREGISTER_CONSTANTS.GET_ALL_REGISTERS] = ACCESSIONREGISTER_CONSTANTS.GET_ALL_REGISTERS;
      } else {
        self.searchCriteria[ACCESSIONREGISTER_CONSTANTS.ORIGINATING_AGENCY_FIELD] = serviceProducerCriteria;
      }

      ihmDemoFactory.getAccessionRegisters(self.searchCriteria)
      .then(
        // Succeeded search request
        function(response) {
          var isReponseValid = responseValidator.validateReceivedResponse(response);
          if (isReponseValid) {
            // Get total results
            self.totalResult = response.data.hits.total;
            self.showResult = true;

            if (self.totalResult > 0) {
              // Display found registers
              self.registers = response.data.result;
            }
          } else {
            // Invalid response
            self.showResult = false;
          }
        },
        // Failed search request
        function(error) {
          self.showResult = false;
        }
      );
    }

    // Default Search
    self.searchRegistersByCriteria(null);
  });

