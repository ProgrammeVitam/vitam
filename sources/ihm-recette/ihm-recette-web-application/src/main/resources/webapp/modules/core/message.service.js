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

// Define service in order to display dialog for operations in popup
angular.module('core')
  .service('messageService', function($filter, $mdDialog, $route) {

    var MessageService = this;

    /**
     * Display an alert message with the given title and message
     *
     * @param title {String} The alert title
     * @param message {String} The alert message
     * @param [okLabel] {String} Optional value for the "OK" button
     */
    MessageService.specificMessageAlert = function(title, message, okLabel) {
      if (!okLabel) {
        okLabel = 'OK';
      }
      var alert = $mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
        .title(title)
        .textContent(message)
        .ok(okLabel);
      $mdDialog.show(alert).then(function(){
        $route.reload();
      });
    };

    /**
     * Display an alert message with the given template
     *
     * @param template {String} Custom template
     * @param locals {Object} Custom variables used in template
     * @param controller {Function} Controller used by the template
     */
    MessageService.specificTemplateMessageAlert = function(template, locals, controller) {
      var alert = {
        parent: angular.element(document.querySelector('#popupContainer')),
        template: template,
        locals: locals,
        controller: controller
      };

      $mdDialog.show(alert).then(function(){
        $route.reload();
      });
    };

    /**
     * Display an alert message that match the given prefix, type and kind of success
     *
     * @param codePrefix {String} The prefix code for the message to be displayed - Should end by a dot (.)
     * @param type {String} The type code for the message to be displayed (can be empty)
     * @param isSuccess {Boolean} true id it's a 'success message', else false.
     *  If not set (undefined), no success nor error suffix is set.
     */
    MessageService.typedMessageAlert = function(codePrefix, type, isSuccess) {
      return function() {
        var successSuffix = (isSuccess !== undefined? (isSuccess? '.success': '.error'): '');
        var title = $filter('translate')(codePrefix + type + '.title' + successSuffix);
        var message = $filter('translate')(codePrefix + type + '.message' + successSuffix);
        MessageService.specificMessageAlert(title, message);
      };
    };

    /**
     * Display a confirm message with the given values and execute the callback if user confirm the action
     *
     * @param title {String} The popup title value
     * @param message {String} The popup content value
     * @param ok {String} the popup ok button message
     * @param cancel {String} the popup cancel button message
     * @param okCallback {Function} The function called if the user confirm the action
     */
    MessageService.specificMessageConfirm = function(title, message, ok, cancel, okCallback) {
      var confirm = $mdDialog.confirm()
        .title(title).textContent(message).ok(ok).cancel(cancel);
      $mdDialog.show(confirm).then(okCallback);
    };

    /**
     * Display a confirm message that match the given prefix, type and ececute the callback if the user confirm
     *
     * @param codePrefix {String} The prefix code for the message to be displayed - Should end by a dot (.)
     * @param type {String} The type code for the message to be displayed
     * @param okCallback {Function} The function called if the user confirm the action
     */
    MessageService.typedMessageConfirm = function(codePrefix, type, okCallback) {
      var title = $filter('translate')(codePrefix + type + '.title');
      var message = $filter('translate')(codePrefix + type + '.message');
      var ok = 'Vider';
      var cancel = 'Annuler';
      MessageService.specificMessageConfirm(title, message, ok, cancel, okCallback);
    }

  });