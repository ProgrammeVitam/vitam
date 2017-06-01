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

// Define resources in order to call WebApp http endpoints for accession-register
angular.module('core')
  .factory('traceabilityOperationResource', function($q,ihmDemoCLient) {

    var TRACEABILITY_OPERATION_DETAIL = '/logbook/operations';
    var EXTRACT_TIMESTAMP = '/traceability/extractTimestamp';
    var CHECK_TRACEABILITY_OPERATION = '/traceability/check';
    var traceabilityOperationResource = {};


    /** Gets traceability operation details by id (GET method)
     *
     * @param {String} id - The requested traceability operation id
     * @returns {HttpPromise} The promise returned by the http call containing traceability operation details
     */
    traceabilityOperationResource.getDetails = function (id) {
      var criteriaSearch = {
        // EventType: 'TRACEABILITY',
        EventID: id
      };

      return ihmDemoCLient.getClient('').all(TRACEABILITY_OPERATION_DETAIL).post(criteriaSearch);
    };

    /** Gets traceability operation verification result (POST method)
     *
     * @param {String} id - The requested traceability operation id to verify
     * @returns {HttpPromise} The promise returned by the http call containing traceability operation verification result
     */
    traceabilityOperationResource.checkTraceabilityOperation = function (id) {
      var criteriaSearch = {
        // EventType: 'TRACEABILITY',
        EventID: id        
      };

      return ihmDemoCLient.getClient('').all(CHECK_TRACEABILITY_OPERATION).post(criteriaSearch);
    };
    
    /** Extract timestamp information
    *
    * @param {String} timestampValue - The requested timestamp 
    * @returns {HttpPromise} The promise returned by the http call containing traceability operation verification result
    */
   traceabilityOperationResource.extractTimeStampInformation = function (timestampValue) {
     var criteriaSearch = {
       timestamp: timestampValue
     };

     return ihmDemoCLient.getClient('').all(EXTRACT_TIMESTAMP).post(criteriaSearch);
   };

    return traceabilityOperationResource;
  });
