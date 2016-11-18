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

angular.module('lifecycle')
  .controller('lifeCycleController', function($routeParams, $filter, ihmDemoFactory, loadStaticValues) {
    var self = this;

    function initFields(fields) {
      var result = [];
      for (var i = 0, len = fields.length; i<len; i++) {
        var fieldId = fields[i];
        result.push({id: fieldId, label: $filter('translate')('lifeCycle.logbook.displaySteps.' + fieldId)});
      }
      return result;
    }

    // ************************************Pagination  **************************** //
    self.viewby = 30;
    self.currentPage = 1;
    self.itemsPerPage = self.viewby;
    self.maxSize = 5;

    self.setPage = function(pageNo) {
      selfcurrentPage = pageNo;
    };

    self.pageChanged = function() {
      console.log('Page changed to: ' + self.currentPage);
    };

    self.setItemsPerPage = function(num) {
      self.itemsPerPage = num;
      self.currentPage = 1; // reset to first page
    };
    // **************************************************************************** //

    // selectedObjects
    self.selectedObjects = [];

    // LifeCycle type
    self.lifeCycleType = $routeParams.type;

    // Archive unit id
    self.lifeCycleId = $routeParams.lifecycleId;

    // Archive unit title
    self.title = $routeParams.lifecycleTitle;

    loadStaticValues.loadFromFile().then(
      function onSuccess(response) {
        var config = response.data;
        self.columnsToDisplay = initFields(config.lifeCycleMandatoryFields);
        self.customFields = initFields(config.lifeCycleCustomFields);
      }, function onError(error) {

      });

    // LifeCycle details
    self.lifeCycleDetails = [];

    // Show result flag
    self.showResult = true;

    // Get lifeCycle details
    var buildLifeCycle = function() {
      ihmDemoFactory.getLifeCycleDetails(self.lifeCycleType, self.lifeCycleId).then(function(response) {
        self.receivedResponse = response;
        if (response.data.$hits === undefined || response.data.$hits === null || response.data.$hits.total !== 1) {
          // Invalid response
          // Display error message
          self.showResult = false;
        } else {
          // Valid response
          self.showResult = true;

          // Build unit LifeCycle details
          // Add just result events
          var lastStartedEvent = '';
          for(var i=0; i < response.data.$results[0].events.length; i++){
            var currentEvent = response.data.$results[0].events[i];
            var nextEvent = response.data.$results[0].events[i + 1];
            var isCurrentAStartEvent = currentEvent.outcome == 'STARTED';
            var currentEventType = currentEvent.evType;
            var isStepLevelEvent = false;

            if(isCurrentAStartEvent){
              lastStartedEvent = currentEventType;

              // Set step level class
              isStepLevelEvent = true;
            } else if(currentEventType == lastStartedEvent){
              // Set step level class
              isStepLevelEvent = true;
            }

            if(nextEvent === undefined || !isCurrentAStartEvent || (currentEventType!==nextEvent.evType)){
              var newEvent = {};
              angular.forEach(currentEvent, function(value, key) {
                var uppercaseKey = key.toUpperCase();
                if (uppercaseKey === 'EVTYPE') {
                  newEvent[uppercaseKey] = $filter('translate')(value);
                } else {
                  newEvent[uppercaseKey] = value;
                }
              });

              // Add class type
              newEvent.isStepLevelEvent = isStepLevelEvent;

              self.lifeCycleDetails.push(newEvent);
            }
          }

          self.totalItems = self.lifeCycleDetails.length;
        }
      }, function(error) {
        // Display error message
        self.showResult = false;
      });
    };

    // Display life cycle
    buildLifeCycle();
  });
