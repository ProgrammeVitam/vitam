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

angular.module('core')
  .service('processResponseService', function($filter, ARCHIVE_UNIT_MODULE_CONST) {

    var self = this;

    // TODO replace me by lowdash function
    var isIn = function(value, array) {
      if (!array)
        return false;

      for(var i = 0; i<array.length; i++) {
        if (value === array[i]) {
          return true;
        }
      }

      return false;
    };

    /**
     * Returns the translated value of the computed key
     * The cumputed key take prefix + parent name (if any) + key
     *
     * @param {String} key - The key of the property
     * @param {Object} parent - The parent key of the property
     * @param {String} prefix - The prefix of the translated property
     * @returns {String} The translated value of the buildedKey if match found or the key.
     */
    self.displayLabel = function(key, parent, prefix) {
      var buildedKey = prefix;
      if (parent !== key) {
        buildedKey = buildedKey + '.' + parent;
      }
      buildedKey = buildedKey + '.' + key;

      return $filter('translate')(buildedKey);
    };

    /**
     * Make an object compatible for field and fieldtree directive from a specific field of the computed object
     *
     * @param {String} value - The value of the computed object
     * @param {String} key - The technical key of the computed object
     * @param {String} parent - The parent of the computed object (Should be key if no parent)
     * @param {Array} parents - Link to the parents of the computed object
     * @param {Array} ignoreValues - The field of the json object that should be ignored (Can be an empty array)
     * @param {String} ignoreStartWith - The first caracter of hidden field that should be ignored (exemple: '_').
     * @param {Boolean} modificationAlowed - true if the fields are editable
     * @param {String} translationPrefix - The commun prefix of the translation key
     * @returns {Object} List of object with technicalId, label, value and child.
     */
    var buildSingleField = function(value, key, parent, parents, ignoreValues, ignoreStartWith, modificationAlowed, translationPrefix) {
      var fieldSet = {};
      var isMgtChild = false;

      fieldSet.fieldId = key;
      fieldSet.fieldValue = value;
      fieldSet.currentFieldValue = value;
      fieldSet.isChild = false;

      // parents list
      fieldSet.parents = [];
      if (parent !== key) {
        fieldSet.parents.push(parent);
        fieldSet.isChild = true;
      }

      fieldSet.fieldName = self.displayLabel(key, parent, translationPrefix);

      if (!angular.isObject(value)) {
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.SIMPLE_FIELD_TYPE;
        if (!isMgtChild) { // FIXME P0 NOSONAR P2 Always true there
          fieldSet.isModificationAllowed = true;
        }
      } else {
        // Composite value
        fieldSet.typeF = ARCHIVE_UNIT_MODULE_CONST.COMPLEX_FIELD_TYPE;
        var contentField = value;
        fieldSet.content = [];

        var keyArrayIndex = 1;
        angular.forEach(contentField, function (value, key) {
          if (!isIn(key, ignoreValues) || key.toString().charAt(0) != ignoreStartWith) {
            var fieldSetSecond = buildSingleField(value, key, fieldSet.fieldId, fieldSet.parents,
              ignoreValues, ignoreStartWith, modificationAlowed, translationPrefix);
            fieldSetSecond.isChild = true;

            if (angular.isArray(contentField)) {
              fieldSetSecond.fieldName = ARCHIVE_UNIT_MODULE_CONST.LIST_ITEM_LABEL + keyArrayIndex;
              keyArrayIndex = keyArrayIndex + 1;
            }

            fieldSet.content.push(fieldSetSecond);
          }
        });
      }
      return fieldSet;
    };

    /**
     * Make an object compatible for field and fieldtree directive from a json object (2 level max)
     *
     * @param {Object} fields - The input json object
     * @param {Array} ignoreValues - The field of the json object that should be ignored (Can be an empty array)
     * @param {String} ignoreStartWith - The first caracter of hidden field that should be ignored (exemple: '_').
     * @param {Boolean} modificationAlowed - true if the fields are editable
     * @param {String} translationPrefix - The commun prefix of the translation key
     * @returns {Array} List of object with technicalId, label, value and child.
     */
    self.buildAllField = function(fields, ignoreValues, ignoreStartWith, modificationAlowed, translationPrefix) {
      var fieldSet = [];

      angular.forEach(fields, function (value, key) {
        if (!modificationAlowed) {
          modificationAlowed = true;
        }
        if (!isIn(key, ignoreValues) || key.toString().charAt(0) != ignoreStartWith) {
          var field = buildSingleField(value, key, key, [], ignoreValues, ignoreStartWith, modificationAlowed, translationPrefix);
          field.isModificationAllowed = modificationAlowed;
          fieldSet.push(field);
        }
      });

      return fieldSet;
    }
  });