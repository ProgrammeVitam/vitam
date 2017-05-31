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

// Define resources in order to call WebApp http endpoints for administration
angular.module('dsl.query.test')
  .constant('IHM_DSL_URLS', {
    'TENANTS': 'tenants',
    'ACCESS_CONTRACTS': 'accesscontracts',
    'DSL_QUERY_TEST': 'dslQueryTest',
    'IHM_BASE_URL':'/ihm-recette/v1/api'
  })
  .factory('dslQueryResource', function($http, IHM_DSL_URLS, tenantService) {

    var DslQueryResource = {};

    /** get tenant of session and put it into header
    *
    * @returns set tenant to header
    */    
    var getTenantHeader = function() { 
    	return {headers : {'X-Tenant-Id' : tenantService.getTenant()}} 
    };

    var getRequestHeader = function (tenantId, contractId, requestedCollection, requestMethod) {
      return { headers : {
        'X-Tenant-Id' : tenantId,
        'X-Access-Contract-Id' : contractId,
        'X-Requested-Collection' : requestedCollection,
        'X-Http-Method-Override' : requestMethod
      }}
    }

    DslQueryResource.getContracts = function() {
      return $http.post(IHM_DSL_URLS.IHM_BASE_URL + "/" + IHM_DSL_URLS.ACCESS_CONTRACTS , '{ContractID: "all", ContractName: "all", orderby: {field: "Name", sortType: "ASC"}}', getTenantHeader());
    };

    DslQueryResource.executeRequest = function(tenantId, contractId, requestedCollection, requestMethod, query, objectId) {
      return $http.post(IHM_DSL_URLS.IHM_BASE_URL + "/" + IHM_DSL_URLS.DSL_QUERY_TEST , query,
                               getRequestHeader(tenantId, contractId, requestedCollection, requestMethod));

    }

    return DslQueryResource;

  });
