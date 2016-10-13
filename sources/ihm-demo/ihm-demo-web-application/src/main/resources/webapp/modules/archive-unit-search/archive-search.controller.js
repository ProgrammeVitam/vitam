/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and
 * efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL 2.1 license
 * as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

angular
 .module('archiveSearch')
 .constant(
  'ARCHIVE_SEARCH_MODULE_CONST', {
   'ARCHIVE_FORM_ALREADY_OPENED': 'Le formulaire de l\'Archive Unit sélectionnée est déjà ouvert.',
   'ARCHIVE_DETAILS_PATH': '#!/archiveunit/',
   'SEARCH_ERROR_MSG': 'Une erreur est survernue lors de la recherche. Veuillez contacter votre administrateur!',
   'ARCHIVE_SEARCH_ERROR_MSG': 'Une erreur est survernue lors de la recherche de l\'unit. Veuillez contacter votre administrateur!',
   'ARCHIVE_NOT_FOUND_MSG': 'L\'archive unit sélectionnée est introuvable.',
   'SEARCH_RESULT_INVALID': 'La réponse reçue est invalide. Impossible d\'afficher le résultat de la recherche.',
   'STARTDATE_GREATER_THAN_ENDDATE': 'La date de début doit être antérieure à la date de fin.',
   'STARTDATE_INVALID': 'Le format de la date de début est invalide. Il doit être du type jj/mm/aaaa',
   'ENDDATE_INVALID': 'Le format de la date de fin est invalide. Il doit être du type jj/mm/aaaa',
   'ONLY_ONE_DATE_SET': 'Une date de début et une date de fin doivent être indiquées.',
   'NO_CRITERIA_SET': 'Aucun résultat. Veuillez entrer au moins un critère de recherche'
  })
 .controller(
  'ArchiveUnitSearchController', [
   '$scope',
   'ihmDemoFactory',
   '$window',
   '$mdToast',
   '$mdDialog',
   'ARCHIVE_SEARCH_MODULE_CONST',
   'archiveDetailsService',
   'dateValidator',
   function($scope, ihmDemoFactory, $window, $mdToast, $mdDialog,
    ARCHIVE_SEARCH_MODULE_CONST, archiveDetailsService, dateValidator) {

    // ******************************* Alert diplayed
    // ******************************* //
    $scope.showAlert = function($event, dialogTitle, message) {
     $mdDialog.show($mdDialog.alert().parent(
       angular.element(document.querySelector('#popupContainer')))
      .clickOutsideToClose(true).title(dialogTitle).textContent(message)
      .ariaLabel('Alert Dialog Demo').ok('OK').targetEvent($event));
    };
    // ****************************************************************************
    // //

    // ***************** Archive Units Search Result
    // ********************** //
    $scope.archiveUnitsSearchResult;
    var criteriaSearch = {};
    $scope.getSearchResult = function getSearchResult($event, titleCriteria) {
     if (titleCriteria !== '' && titleCriteria !== null && titleCriteria !== undefined) {
      // Build title criteria and default
      // selection
      criteriaSearch.titleAndDescription = titleCriteria;
      criteriaSearch.projection_transactdate = "TransactedDate";
      criteriaSearch.projection_id = "#id";
      criteriaSearch.projection_title = "Title";
      criteriaSearch.projection_object = "#object";
      criteriaSearch.orderby = "TransactedDate";
      criteriaSearch.isAdvancedSearchFlag = "No";

      ihmDemoFactory
       .searchArchiveUnits(criteriaSearch)
       .then(
        function(response) {

         if (response.data.$result == null || response.data.$result == undefined || response.data.$hint == null || response.data.$hint == undefined) {
          $scope.error = true;
          $scope.showResult = false;
          $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.SEARCH_RESULT_INVALID;
         } else {
          $scope.archiveUnitsSearchResult = response.data.$result;
          $scope.showResult = true;
          $scope.error = false;

          // Set Total result
          $scope.totalResult = response.data.$hint.total;

          // ************************************Pagination
          // ****************************
          // //
          $scope.totalItems = $scope.archiveUnitsSearchResult.length;
          // ****************************************************************************
          // //
         }

        },
        function(error) {
         console.log('Error retrieving archive units! ' + error.message);
         $scope.error = true;
         $scope.showResult = false;
         $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.SEARCH_ERROR_MSG;
         $scope.showAlert($event, "Erreur", $scope.errorMessage);
        });
     } else {
       $scope.showResult = false;
       $scope.showAlert($event, "Info", ARCHIVE_SEARCH_MODULE_CONST.NO_CRITERIA_SET);
     }
    };

    // Display Selected Archive unit form
    $scope.openedArchiveId = [];
    $scope.openedArchiveWindowRef = [];
    $scope.archiveDetailsConfig = null;
    $scope.displayArchiveUnitForm = function displayArchiveUnitForm($event,
     archiveId) {

     var openArchiveDetailsWindow = function(data) {
      // Start form diplaying
      var archiveUnitwindow;
      var archiveUnitwindowIndex = $scope.openedArchiveId.indexOf(archiveId);
      if (archiveUnitwindowIndex !== -1) {
       archiveUnitwindow = $scope.openedArchiveWindowRef[archiveUnitwindowIndex];

       // Show Toast to indicate that the
       // archive unit form is
       // already opened
       $scope.infoMessage = ARCHIVE_SEARCH_MODULE_CONST.ARCHIVE_FORM_ALREADY_OPENED;
       $scope.showAlert($event, "Info", $scope.infoMessage);
      } else {

       archiveUnitwindow = $window
        .open(ARCHIVE_SEARCH_MODULE_CONST.ARCHIVE_DETAILS_PATH + archiveId);
       archiveUnitwindow.data = data[0];
       archiveUnitwindow.dataConfig = $scope.archiveDetailsConfig;

       // add a close listener to the window to
       // update
       // $scope.openedArchiveId content
       archiveUnitwindow.onbeforeunload = function() {
        $scope.openedArchiveId.splice(archiveUnitwindowIndex, 1);
       }
       $scope.openedArchiveId.push(archiveId);
       $scope.openedArchiveWindowRef.push(archiveUnitwindow);
      }
     };

     var displayFormCallBack = function(data) {
      if (data.$result == null || data.$result == undefined || data.$hint == null || data.$hint == undefined) {
       $scope.error = true;
       $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.SEARCH_RESULT_INVALID;
       $scope.showAlert($event, "Erreur", $scope.errorMessage);
      } else {
       $scope.totalFoundArchive = data.$hint.total;
       if ($scope.totalFoundArchive != 1) {
        console.log('Archive unit not found');
        $scope.error = true;
        $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.ARCHIVE_NOT_FOUND_MSG;
        $scope.showAlert($event, "Erreur", $scope.errorMessage);
       } else {
        // Archive unit found
        // Get Archive Details configuration
        // only once
        if ($scope.archiveDetailsConfig == null || $scope.archiveDetailsConfig == undefined) {
         // $scope.archiveDetailsConfig =
         // archiveDetailsService.getArchiveUnitDetailsConfig();
         ihmDemoFactory.getArchiveUnitDetailsConfig()
          .then(
           function(response) {
            $scope.archiveDetailsConfig = response.data;
            openArchiveDetailsWindow(data.$result);
           },
           function(error) {
            console
             .log(ARCHIVE_UNIT_MODULE_CONST.CONFIG_FILE_NOT_FOUND_MSG);
            $scope.archiveDetailsConfig = {};
            openArchiveDetailsWindow(data.$result);
           });
        } else {
         openArchiveDetailsWindow(data.$result);
        }
       }
      }
     };

     var failureCallback = function(errorMsg) {
      // Display error message
      console.log(errorMsg);
      $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.ARCHIVE_SEARCH_ERROR_MSG;
      $scope.showAlert($event, "Erreur", $scope.errorMessage);
     }

     // Find archive unit details
     archiveDetailsService.findArchiveUnitDetails(archiveId,
      displayFormCallBack, failureCallback);
    }

    $scope.isObjectExist = function isObjectExist(object) {
     if (object !== null && object !== undefined && !angular.equals(object, "")) {
      return true;
     } else {
      return false;
     }
    };

    // ************************************Pagination
    // **************************** //
    $scope.viewby = 10;
    $scope.currentPage = 1;
    $scope.itemsPerPage = $scope.viewby;
    $scope.maxSize = 5;

    $scope.setPage = function(pageNo) {
     $scope.currentPage = pageNo;
    };

    $scope.pageChanged = function() {
     console.log('Page changed to: ' + $scope.currentPage);
    };

    $scope.setItemsPerPage = function(num) {
      $scope.itemsPerPage = num;
      $scope.currentPage = 1; // reset to first page
     }
     // ****************************************************************************
     // //

    // ***************** Archive Units Search Result
    // ********************** //
    $scope.getElasticSearchUnitsResult = function getElasticSearchUnitsResult(
      $event, id, title, description, startDate, endDate) {
      $scope.criteriaSearch = {};
      var atLeastOneValidCriteriaExists = false;
      var atLeastOneCriteriaExists = false;

      if (id !== '' && id !== null && id !== undefined) {
         // Add title to criteria
         atLeastOneValidCriteriaExists = true;
         atLeastOneCriteriaExists = true;
         $scope.criteriaSearch._id = id;
      } else {
         if (title !== '' && title !== null && title !== undefined) {
            // Add title to criteria
            atLeastOneValidCriteriaExists = true;
            atLeastOneCriteriaExists = true;
            $scope.criteriaSearch.Title = title;
         }

         if (description !== '' && description !== null && description !== undefined) {
            // Add description to criteria
            atLeastOneValidCriteriaExists = true;
            atLeastOneCriteriaExists = true;
            $scope.criteriaSearch.Description = description;
         }

         // Control dates
         var isStartDateSet = startDate !== '' && startDate !== null && startDate !== undefined;
         var isEndDateSet = endDate !== '' && endDate !== null && endDate !== undefined;

         var isValidStartDate = isStartDateSet && dateValidator.validateDate(startDate);
         var isValidEndDate = isEndDateSet && dateValidator.validateDate(endDate);

         if (isStartDateSet || isEndDateSet) {
            atLeastOneCriteriaExists = true;
            if ((isStartDateSet && !isEndDateSet) || !isStartDateSet) {
               atLeastOneValidCriteriaExists = false;
               $scope.showAlert($event, "Erreur", ARCHIVE_SEARCH_MODULE_CONST.ONLY_ONE_DATE_SET);
            } else if (isValidStartDate && isValidEndDate) {
               // Check if startDate <= endDate
               var startDateParts = $scope.startDate.split('/');

               // new Date(year, month [, day [,
               // hours[, minutes[,
               // seconds[, ms]]]]])
               var startDateAsDate = new Date(startDateParts[2], startDateParts[1] - 1, startDateParts[0], "00", "00", "00");
               var endDateParts = $scope.endDate.split('/');
               var endDateAsDate = new Date(endDateParts[2], endDateParts[1] - 1,
                endDateParts[0], "23", "59", "59");

               if (startDateAsDate <= endDateAsDate) {
                  atLeastOneValidCriteriaExists = true;
                  $scope.criteriaSearch.StartDate = startDateAsDate;
                  $scope.criteriaSearch.EndDate = endDateAsDate;
               } else {
                  atLeastOneValidCriteriaExists = false;
                  $scope.showAlert($event, "Erreur", ARCHIVE_SEARCH_MODULE_CONST.STARTDATE_GREATER_THAN_ENDDATE);
               }
            } else if (!isValidStartDate) {
               atLeastOneValidCriteriaExists = false;
               $scope.showAlert($event, "Erreur", ARCHIVE_SEARCH_MODULE_CONST.STARTDATE_INVALID);
            } else if (!isValidEndDate) {
               atLeastOneValidCriteriaExists = false;
               $scope.showAlert($event, "Erreur", ARCHIVE_SEARCH_MODULE_CONST.ENDDATE_INVALID);
            }
         }
      }


      if (atLeastOneValidCriteriaExists) {
       $scope.criteriaSearch.projection_transactdate = "TransactedDate";
       $scope.criteriaSearch.projection_id = "#id";
       $scope.criteriaSearch.projection_title = "Title";
       $scope.criteriaSearch.projection_object = "#object";
       $scope.criteriaSearch.orderby = "TransactedDate";
       $scope.criteriaSearch.isAdvancedSearchFlag = "Yes";

       ihmDemoFactory
        .searchArchiveUnits($scope.criteriaSearch)
        .then(
         function(response) {

          if (response.data.$result == null || response.data.$result == undefined || response.data.$hint == null || response.data.$hint == undefined) {
           $scope.error = true;
           $scope.showResult = false;
           $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.SEARCH_RESULT_INVALID;
           $scope.showAlert($event, "Erreur", $scope.errorMessage);
          } else {
           $scope.archiveUnitsSearchResult = response.data.$result;
           $scope.showResult = true;
           $scope.error = false;

           // Set Total result
           $scope.totalResult = response.data.$hint.total;

           // ******************************** Pagination ****************************** //
           $scope.totalItems = $scope.archiveUnitsSearchResult.length;
           // ************************************************************************** //

          }
         },
         function(error) {
          console.log('Error retrieving archive units! ' + error.message);
          $scope.error = true;
          $scope.showResult = false;
          $scope.errorMessage = ARCHIVE_SEARCH_MODULE_CONST.SEARCH_ERROR_MSG;
          $scope.showAlert($event, "Erreur", $scope.errorMessage);
         });
      } else if(!atLeastOneCriteriaExists) {
         // Clear old result eventually
         $scope.showResult = false;
         $scope.showAlert($event, "Info", ARCHIVE_SEARCH_MODULE_CONST.NO_CRITERIA_SET);
      }
     }
     // ***************************************************************************
     // //

   }
  ]);
