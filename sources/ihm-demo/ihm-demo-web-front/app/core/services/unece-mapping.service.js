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

/**
 * Service for handle unece unit
 */

angular.module("core").constant("_", window._);
angular.module('core')
  .service('uneceMappingService', function(_, $http, $filter) {

    var UneceMappingService = {};

    var uneceCode = [];
    var filePath = 'static/unece.json';
    var promise = null;
    var translateKey = 'archiveunit.physical.fields.';

    var initFile = function() {
      // File loaded only on the first call (change page or refresh page (F5) will fire another HTTP call
      if (promise === null) {
        promise = $http.get(filePath);
      }

      return promise;
    };

    var canDoGrammaticalAgreementForUnit = function(dimensionValue) {
      return dimensionValue >= 2;

    };

    /**
     *
     * @param {Object} dimension - contains value and unit for the dimension
     * @param {Object} dimensionKind - dimension Kind of the object can be (height, width, ect...)
     *
     * @returns {Object} new object that contains : dimension key, dimension value and dimension label to display,
     * if the dimension.unit it's null, undefined or not matching value return object without dimension label
     */
    UneceMappingService.getDimensionWithDisplayableUnit = function(dimension, dimensionKind) {
      var translatedKind = '';
      if (!!dimensionKind) {
        translatedKind = $filter('translate')(translateKey + dimensionKind);
      }
      if (typeof dimension === 'object') {

        return initFile()
          .then(
          function onSuccess(response) {
            var config = response.data;
            uneceCode = _.keys(config);
            if (_.indexOf(uneceCode, dimension.unit) === -1) {
              return {key: translatedKind, value: dimension.value};
            } else {
              if (canDoGrammaticalAgreementForUnit(dimension.value)) {
                return {key: translatedKind, value: dimension.value + ' ' + config[dimension.unit] + 's'};
              } else {
                return {key: translatedKind, value: dimension.value + ' ' + config[dimension.unit]};
              }
            }
          }, function onError(error) {
            return null;
          });
      } else {
        return {key: translatedKind, value: dimension};
      }
    };

    return UneceMappingService;
  });