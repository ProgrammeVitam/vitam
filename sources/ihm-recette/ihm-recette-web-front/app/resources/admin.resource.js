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
angular.module('core')
  .factory('adminResource', function($http, IHM_URLS, tenantService) {

    var DELETE_ROOT = '/delete';
    var FORMAT_ROOT = '/formats/';
    var RULE_ROOT = '/rules/';
    var ACCESSION_REGISTER_ROOT = '/accessionregisters';
    var LOGBOOK_ROOT = '/logbook/operation';
    var OBJECT_GROUP_LIFE_CYCLE_ROOT = '/logbook/lifecycle/objectgroup';
    var UNIT_LIFE_CYCLE_ROOT = '/logbook/lifecycle/unit';
    var OBJECT_GROUP_ROOT = '/metadata/objectgroup';
    var ARCHIVE_UNIT_ROOT = '/metadata/unit';
    var AdminResource = {};

    /** get tenant of session and set to header
    *
    * @returns set tenant to header
    */    
    var getTenantHeader = function() { 
    	return {headers : {'X-Tenant-Id' : tenantService.getTenant()}} 
    };
    
    /** Delete all the formats (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteFormats = function () {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + FORMAT_ROOT);
    };

    /** Delete all the rules (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteRules = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + RULE_ROOT, getTenantHeader());
    };

    /** Delete all the accession register (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteAccessionRegisters = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + ACCESSION_REGISTER_ROOT, getTenantHeader());
    };

    /** Delete all the logbooks (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteLogbooks = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + LOGBOOK_ROOT, getTenantHeader());
    };

    /** Delete all the archive unit lifeCycle (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteUnitLifeCycles = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + UNIT_LIFE_CYCLE_ROOT, getTenantHeader());
    };

    /** Delete all the object group life cycles (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteOGLifeCycles = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + OBJECT_GROUP_LIFE_CYCLE_ROOT, getTenantHeader());
    };

    /** Delete all the archive units (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteArchiveUnits = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + ARCHIVE_UNIT_ROOT,  getTenantHeader());
    };

    /** Delete all the object groups (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteObjectGroups = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT + OBJECT_GROUP_ROOT, getTenantHeader());
    };

    /** Delete all the mongo collections (DELETE method)
     *
     * @returns {HttpPromise} The promise returned by the http call
     */
    AdminResource.deleteAll = function() {
      return $http.delete(IHM_URLS.IHM_BASE_URL + DELETE_ROOT, getTenantHeader());
    };

    return AdminResource;

  });
