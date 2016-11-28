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

// Define controller for admin_home
angular.module('admin.home')
  .controller('adminHomeController', function($scope, $filter, adminService, messageService) {

    /**
     * Prefix code for callback message of this page
     * @type {string}
     */
    var CODE_PREFIX = 'admin.delete.';

    /**
     * Prefix code for delete confirmation popup
     * @type {string}
     */
    var CONFIRM_PREFIX = 'confirm.admin.delete.';

    /**
     * Format and display an error with the list of deletion failure
     *
     * @param response The http response
     */
    var displayDeleteAllError = function(response) {
    	
      var template = '<md-dialog aria-label="List dialog" class="md-default-theme">' +
        '  <md-dialog-content class="md-dialog-content">'+
        '    <h2 class="md-title ng-binding">{{title}}</h2>' +
        '    <md-list>'+
        '      <div class="_md-dialog-content-body ng-scope">' +
        '      <p class="ng-binding">{{message}}</p>' +
        '      <md-list-item ng-repeat="error in errorList">'+
        '       <p>{{error}}</p>' +
        '      </md-list-item>' +
        '    </md-list>'+
        '  </md-dialog-content>' +
        '  <md-dialog-actions>' +
        '    <button ng-click="closeDialog()" class="md-primary md-button md-default-theme md-ink-ripple" md-autofocus="true">OK</button>' +
        '  </md-dialog-actions>' +
        '</md-dialog>';

      var locals = {
        errorList: response.data,
        title: $filter('replaceDoubleQuote')($filter('translate')(CODE_PREFIX + 'all.title.error')),
        message: $filter('replaceDoubleQuote')($filter('translate')(CODE_PREFIX + 'all.message.error'))
      };

      var controller = function($scope, $mdDialog, errorList, message, title) {
        $scope.errorList = errorList;
        $scope.message = message;
        $scope.title = title;
        $scope.closeDialog = function() {
          $mdDialog.hide();
        }
      };

      messageService.specificTemplateMessageAlert(template, locals, controller);
    };

    /**
     * Display a delete confirmation message and launch deletion if user confirm.
     *
     * @param deleteFunction {Function} The deletion function called if user confirm action
     * @param deleteMessageCode {String} The message code corresponding to the type of item how must be deleted
     */
    $scope.deleteConfirm = function(deleteFunction, deleteMessageCode) {
      if (messageService.typedMessageConfirm(CONFIRM_PREFIX, deleteMessageCode, deleteFunction)) {
        deleteFunction();
      }
    };

    $scope.deleteFormats = function () {
      adminService.deleteFileFormat(
        messageService.typedMessageAlert(CODE_PREFIX, 'format', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'format', false)
      );
    };

    $scope.deleteRules = function () {
      adminService.deleteRulesFile(
        messageService.typedMessageAlert(CODE_PREFIX, 'rule', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'rule', false)
      );
    };

    $scope.deleteAccessionRegisters = function () {
      adminService.deleteAccessionRegisters(
        messageService.typedMessageAlert(CODE_PREFIX, 'accessionRegister', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'accessionRegister', false)
      );
    };

    $scope.deleteLogbooks = function () {
      adminService.deleteLogbooks(
        messageService.typedMessageAlert(CODE_PREFIX, 'logbook', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'logbook', false)
      );
    };

    $scope.deleteUnitLifeCycles = function () {
      adminService.deleteUnitLifeCycles(
        messageService.typedMessageAlert(CODE_PREFIX, 'unitLifeCycle', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'unitLifeCycle', false)
      );
    };

    $scope.deleteOGLifeCycles = function () {
      adminService.deleteOGLifeCycles(
        messageService.typedMessageAlert(CODE_PREFIX, 'objectGroupLifeCycle', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'objectGroupLifeCycle', false)
      );
    };

    $scope.deleteArchiveUnits = function() {
      adminService.deleteArchiveUnits(
        messageService.typedMessageAlert(CODE_PREFIX, 'archiveUnit', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'archiveUnit', false)
      );
    };

    $scope.deleteObjectGroups = function () {
      adminService.deleteObjectGroups(
        messageService.typedMessageAlert(CODE_PREFIX, 'objectGroup', true),
        messageService.typedMessageAlert(CODE_PREFIX, 'objectGroup', false)
      );
    };

    $scope.deleteAll = function() {
      adminService.deleteAll(
        messageService.typedMessageAlert(CODE_PREFIX, 'all', true),
        displayDeleteAllError
      );
    };

  });
