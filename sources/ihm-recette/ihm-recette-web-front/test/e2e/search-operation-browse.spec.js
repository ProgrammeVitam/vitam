/*
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

describe('Traceability operation search and browse', function() {
  var logInlogOutUtilsService = require('./utils/login-logout.functions.js');
  var genericUtilsService = require('./utils/generic-utils.function');
  var traceabilityId = 'ID';
  var fullBreadcrumb = [
    'Menu',
    'Recherche d\'un journal sécurisé',
    'Détail du journal sécurisé'
  ];

  beforeAll(function() {
    if(browser.params.mock === true) {
      browser.addMockModule('httpMocker', function () {
        angular.module('httpMocker', ['ngMockE2E'])
          .run(function ($httpBackend) {
            var responseEncapsulation = {$hits:{"total":1,"offset":0,"limit":1,"size":1},$results:[],$context:{"$query":{"$and":[{"$exists":"OriginatingAgency"}]},"$filter":{"$orderby":{"OriginatingAgency":1}},"$projection":{}}};
            var searchOperationResult = {
              "_id": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
              "evId": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
              "evType": "LOGBOOK_OP_SECURISATION",
              "evDateTime": "2017-02-10T10:41:58.130",
              "evDetData": null,
              "evIdProc": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
              "evTypeProc": "TRACEABILITY",
              "outcome": "STARTED",
              "outDetail": "LOGBOOK_OP_SECURISATION.STARTED",
              "outMessg": "Début de la sécurisation des journaux",
              "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"logbook\",\"PlatformId\":425367}",
              "agIdApp": null,
              "evIdAppSession": null,
              "evIdReq": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
              "agIdSubm": null,
              "agIdOrig": null,
              "obId": null,
              "obIdReq": null,
              "obIdIn": null,
              "events": [{
                "evId": "aedqaaaaacaam7mxaah6gak2e6o2k4qaaaaq",
                "evType": "STP_OP_SECURISATION",
                "evDateTime": "2017-02-10T10:42:07.346",
                "evDetData": "{\"StartDate\":\"-999999999-01-01T00:00:00\",\"EndDate\":\"2017-02-10T10:41:58.173\",\"Hash\":\"hash\",\"TimeStampToken\":\"token\",\"NumberOfElements\":1828,\"FileName\":\"0_LogbookOperation_20170210_104158.zip\"}",
                "evIdProc": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
                "evTypeProc": "TRACEABILITY",
                "outcome": "OK",
                "outDetail": "STP_OP_SECURISATION.OK",
                "outMessg": "Succès du processus de sécurisation des journaux",
                "agId": {
                  "Name": "vitam-iaas-app-01",
                  "Role": "logbook",
                  "PlatformId": 425367
                },
                "evIdReq": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
                "obId": null,
                "obIdReq": null,
                "obIdIn": null
              }]
            };

            // Mock login http call
            $httpBackend.whenPOST(/ihm-recette\/v1\/api\/login/)
              .respond(200, {});

            $httpBackend.whenPOST(/ihm-recette\/v1\/api\/logbooks/)
              .respond(function(type, uri, data) {
                var json = JSON.parse(data);
                var response = responseEncapsulation;
                if (json.TraceabilityId === 'UNKNOW_ID_FOR_TEST') {
                  // Mock second getSummary call with BAD_ARG
                  response = responseEncapsulation;
                  response.$hits.total = 0;
                  response.$hits.size = 0;
                  response.$results = [];
                } else {
                  // Mock third getSummary call with GOOD_ARG
                  response = responseEncapsulation;
                  response.$results = [searchOperationResult];
                  if (!!json.TraceabilityId) {
                    response.$results[0].TraceabilityId = json.TraceabilityId;
                  }
                }
                return [200, response];
              });


            // Mock getDetails
            $httpBackend.whenPOST(/logbooks\/aecaaaaaacaam7mxaaulkak2gshoa7iaaaaq/)
              .respond(function() {
                var response = responseEncapsulation;
                response.$results = [searchOperationResult];
                return [200, response];
              });

            // Mock logbook messages
            $httpBackend.whenGET(/ihm-recette\/v1\/api\/messages\/logbook/).respond(200, {});

            // Ignore static resources call (html/json)
            $httpBackend.whenGET(/views\/login.html/).passThrough();
            $httpBackend.whenGET(/admin-home.template.html/).passThrough();
            $httpBackend.whenGET(/search-operation.template.html/).passThrough();
            $httpBackend.whenGET(/detailOperation.html/).passThrough();
            $httpBackend.whenGET(/\.json$/).passThrough();
          })
      });
    }

    logInlogOutUtilsService.doLogin(browser, element, by);
  });

  afterAll(function() {
    logInlogOutUtilsService.doLogout(element, by);
  });

  it('should go to the traceability operation search', function () {
    function doTheJob() {
      // Get search link and launch search
      var navBar = element(by.id('navbar'));
      var ul = navBar.element(by.css('[class="nav navbar-nav"]'));
      // FIXME : Get the 0th index menu because by.css('[class="@@mainMenu1 dropdown"]') don't work.
      var li = ul.all(by.css('.dropdown')).get(0);
      li.getAttribute('class').then(function (value) {
        expect(value).toBe('@@mainMenu1 dropdown');
      });

      // Open the dropbox and click the search link
      li.element(by.css('[class="block dropdown-toggle"]')).click();
      li.element(by.css('[href="#!/searchOperation"]')).click();
      expect(browser.getCurrentUrl()).toMatch(/.*\/searchOperation/);
    }

    browser.get(browser.baseUrl + '/').then(doTheJob);
  });

  it('should search automatically and return all result', function() {
    // FIXME Be sure that at least one result is present when tests are launched (for non-mocking mode)
    browser.get(browser.baseUrl + '/searchOperation');
    expect(browser.getCurrentUrl()).toMatch(/.*\/searchOperation/);

    // Check automatic search is launched
    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));

    expect(trs.count()).not.toEqual(0);
    var firstResult = trs.first();
    expect(firstResult.isPresent()).toBeTruthy();

    // FIXME Can't handle this code because no ID is displayed in the table
    /*var firstColumn = firstResult.all(by.css('td')).first();
     firstColumn.getText().then(function(value) {
     traceabilityId = value;

     // Click on a result and check the destination location
     firstResult.click().then(function () {
     browser.getAllWindowHandles().then(function (handles) {
     var newWindowHandle = handles[1]; // this is your new window
     browser.switchTo().window(newWindowHandle).then(function () {
     var regex = new RegExp(".*\/searchOperation/detailOperation\/" + traceabilityId, "");
     expect(browser.getCurrentUrl()).toMatch(regex);
     browser.close();
     browser.switchTo().window(handles[0]);
     });
     });
     });
     });*/
  });

  it('shouldn\'t throw result for wrong parameters', function() {
    browser.get(browser.baseUrl + '/searchOperation');
    expect(browser.getCurrentUrl()).toMatch(/.*\/searchOperation/);
    // TODO Be sure that this keyword is not used for any Origating Agency (for non-mocking mode)
    var keyword = 'UNKNOW_ID_FOR_TEST';
    element(by.model('searchId')).sendKeys(keyword);
    element(by.css('[type="submit"]')).click();

    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));
    // Check only 1 result is present
    expect(trs.count()).toBe(0);
    var searchResults = element(by.css('[data-target="#boxEntriesList"]'));
    var searchString = searchResults.element(by.css('h2')).element(by.css('span'));
    expect(searchString.getText()).toBe('(0)');
  });

  it('should update search when press ENTER key', function() {
    browser.get(browser.baseUrl + '/searchOperation');
    expect(browser.getCurrentUrl()).toMatch(/.*\/searchOperation/);
    // FIXME Update (dynamic ?) keyword for a correspondance with an ingested SIP (for non-mocking mode) and with unique result
    var keyword = 'OAIFERNANDES';
    element(by.model('searchId')).sendKeys(keyword);
    element(by.model('searchId')).sendKeys('\n');

    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));
    // Check only 1 result is present
    expect(trs.count()).toBe(1);
    var searchResults = element(by.css('[data-target="#boxEntriesList"]'));
    var searchString = searchResults.element(by.css('h2')).element(by.css('span'));
    expect(searchString.getText()).toBe('(1)');
  });

  it('should display the good information about result', function() {

    browser.get(browser.baseUrl + '/searchOperation/detailOperation/' + traceabilityId);

    var regex = new RegExp(".*\/searchOperation/detailOperation\/" + traceabilityId, "");
    expect(browser.getCurrentUrl()).toMatch(regex);

    var titleSpans = element.all(by.css('[class="panel-header"]'));
    var h2Title = titleSpans.get(0).element(by.css('h2'));
    expect(h2Title.getText()).toBe(('détail de l\'opération - ' + traceabilityId).toUpperCase());

  });

  it('should browse to good pages with search operation breadcrumb', function() {
    browser.get(browser.baseUrl + '/searchOperation/detailOperation/' + traceabilityId);
    var regex = new RegExp(".*\/searchOperation/detailOperation\/" + traceabilityId, "");
    expect(browser.getCurrentUrl()).toMatch(regex);
    genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 0, fullBreadcrumb[0]);
    genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 1, fullBreadcrumb[1]);
    genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 2, fullBreadcrumb[2]);

    var breadcrumb = element(by.css('[class="breadcrumb"]'));
    var checkingPart = breadcrumb.all(by.css('li')).get(1);
    checkingPart.click();
    genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 0, fullBreadcrumb[0]);
    genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 1, fullBreadcrumb[1]);
  });

});
