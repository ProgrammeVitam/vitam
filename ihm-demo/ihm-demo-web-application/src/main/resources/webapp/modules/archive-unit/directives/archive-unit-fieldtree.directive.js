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

/*
  used in order to display a set of fields. This directive allow update with edit-mode

  It takes some mandatory parameters:
    field-object: The fields object (fieldSet for example)
    edit-mode: true if the view is in edition mode ($ctrl.isEditMode for example)
    intercept-user-change: Callback function to be called when a user update the input field in edit mode
      The callback should take exactly one parameter called fieldSet as this example: intercept-user-change="callback(fieldSet)"
 */
angular.module('archive.unit')
  .directive('archiveUnitFieldtree', function() {
    return {
      scope: {
        fieldObject: '=fieldObject',
        editMode: '=editMode',
        interceptUserChange: '&interceptUserChange'
      },
      templateUrl: 'modules/archive-unit/directives/archive-unit-fieldtree.directive.html'
    };
  });
