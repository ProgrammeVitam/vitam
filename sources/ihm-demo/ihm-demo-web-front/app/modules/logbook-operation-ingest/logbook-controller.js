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
 
'use strict';

angular.module('ihm.demo')
  .constant('ITEM_PER_PAGE', 25)
  .constant('MAX_REQUEST_ITEM_NUMBER', 125)
  .filter('startFrom', function() {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
  })
  .filter('replaceDoubleQuote', function() {
      return function (input) {
        if (!!input) {
          return input.replace(/\'\'/g, '\'');
    	}
        return input;
      }
    })
  .controller('logbookController', function($scope, $window, ihmDemoCLient, ITEM_PER_PAGE, MAX_REQUEST_ITEM_NUMBER) {
    var ctrl = this;
    ctrl.itemsPerPage = ITEM_PER_PAGE;
    ctrl.currentPage = 1;
    ctrl.startDate = new Date();
    ctrl.endDate = new Date();
    ctrl.searchOptions = {};
    ctrl.resultPages = '';
    ctrl.logbookEntryList = [];
    ctrl.pageActive = [true, false, false, false, false];
    ctrl.client = ihmDemoCLient.getClient('logbook');
    var header = {'X-Limit': MAX_REQUEST_ITEM_NUMBER};
    ctrl.noResult = false;

    function displayError(message) {
      ctrl.noResult = true;
      ctrl.errorMessage = message;
      $timeout(function() {
        ctrl.noResult = false;
      }, 5000);
    }

    ctrl.getLogbooks = function() {
      ctrl.searchOptions.INGEST = "all";
      ctrl.searchOptions.orderby = "evDateTime";
      if(ctrl.searchOptions.obIdIn === ""){
    	  delete ctrl.searchOptions.obIdIn;
      }
      ctrl.client.all('operations').customPOST(ctrl.searchOptions, null, null, header).then(function(response) {
        if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
          ctrl.results = 0;
          displayError("Il n'y a aucun résultat pour votre recherche");
          return;
        }
        ctrl.logbookEntryList = response.data.$results;
        ctrl.logbookEntryList.map(function(item) {
          item.obIdIn = ctrl.searchOptions.obIdIn;
        });
        ctrl.currentPage = 1;
        ctrl.results = response.data.$hits.total;
     //   ctrl.diplayPage = ctrl.diplayPage || ctrl.currentPage;
        header['X-REQUEST-ID'] = response.headers('X-REQUEST-ID');
        ctrl.resultPages = Math.ceil(ctrl.results/ctrl.itemsPerPage);
      }, function(response) {
        ctrl.searchOptions = {};
        ctrl.results = 0;
      });
    };

      ctrl.startFormat = function(){
        var start="";

        if(ctrl.currentPage > 0 && ctrl.currentPage <= ctrl.resultPages){
          start= (ctrl.currentPage-1)*ctrl.itemsPerPage;
        }

        if(ctrl.currentPage>ctrl.resultPages){
          start= (ctrl.resultPages-1)*ctrl.itemsPerPage;
        }
        return start;
      };

      ctrl.downloadObject = function(objectId, type) {
          ihmDemoCLient.getClient('ingests').one(objectId).one(type).get().then(function(response) {
              var a = document.createElement("a");
              document.body.appendChild(a);

              var url = URL.createObjectURL(new Blob([response.data], { type: 'application/xml' }));
              a.href = url;

              if(response.headers('content-disposition')!== undefined && response.headers('content-disposition')!== null){
                a.download = response.headers('content-disposition').split('filename=')[1];
                a.click();
              }
          }, function(response) {
            $mdDialog.show($mdDialog.alert().parent(
              angular.element(document.querySelector('#popupContainer')))
              .clickOutsideToClose(true).title('Téléchargement erreur').textContent('Non disponible en téléchargement')
              .ariaLabel('Alert Dialog Demo').ok('OK'));
          });
        };

    ctrl.clearSearchOptions = function() {
      ctrl.searchOptions = {};
    };

    ctrl.diplayFromCurrentPage = function(id) {
      ctrl.diplayPage = id + 1;
      changePageDIsplayNumber(id);
    };

    ctrl.getPreviousResults = function() {
      if (ctrl.currentPage > 1 ) {
        ctrl.diplayPage -= 1;
        if (ctrl.diplayPage == 0 &&  ctrl.currentPage > 5) {
          ctrl.logbookEntryList = [];
          ctrl.currentPage -=5;
          ctrl.diplayPage = 5;
          header['X-Offset'] = (ctrl.currentPage-1) * ITEM_PER_PAGE;
          ctrl.getLogbooks();
        }
        changePageDIsplayNumber((ctrl.diplayPage-1)%5);
      } else if (ctrl.diplayPage > ctrl.currentPage) {
        ctrl.diplayPage -= 1;
        changePageDIsplayNumber((ctrl.diplayPage-1)%5);
      }
    };

    function changePageDIsplayNumber(id) {
      ctrl.pageActive = [false, false, false, false, false];
      ctrl.pageActive[id] = true;
    }

    ctrl.getNextResults = function() {
      if (ctrl.currentPage+4 < ctrl.resultPages) {
        ctrl.diplayPage +=1;
        if (ctrl.diplayPage > 5) {
          ctrl.logbookEntryList = [];
          ctrl.currentPage += 5;
          ctrl.diplayPage = 1;
          header['X-Offset'] = (ctrl.currentPage-1) * ITEM_PER_PAGE;
          ctrl.getLogbooks();
        }
        changePageDIsplayNumber((ctrl.diplayPage-1)%5);
      } else if (ctrl.diplayPage < ctrl.resultPages && (ctrl.diplayPage + ctrl.currentPage) < ctrl.resultPages) {
        ctrl.diplayPage +=1;
        changePageDIsplayNumber((ctrl.diplayPage-1)%5);
      }
    };

      ctrl.goToDetails = function(id) {
        $window
            .open('#!/admin/logbookOperations/'
            + id)
      };

    ctrl.getLogbooks();

  });