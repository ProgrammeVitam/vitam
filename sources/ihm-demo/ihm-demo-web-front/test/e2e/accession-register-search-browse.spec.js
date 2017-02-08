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

describe('AccessionRegister search and browse', function() {
  var logInlogOutUtilsService = require('./utils/login-logout.functions.js');
  var genericUtilsService = require('./utils/generic-utils.function');
  var accessionRegisterId = null;
  var accessionRegisterId = 'ACCESSIONREGISTER';
  var fullBreadcrumb = [
    'Recherche',
    'Recherche Registre des Fonds',
    'Détail du Fonds'
  ];

  beforeAll(function() {
    if(browser.params.mock === true) {
      browser.addMockModule('httpMocker', function () {
        angular.module('httpMocker', ['ngMockE2E'])
          .run(function ($httpBackend) {
            var responseEncapsulation = {$hits:{"total":1,"offset":0,"limit":1,"size":1},$results:[],$context:{"$query":{"$and":[{"$exists":"OriginatingAgency"}]},"$filter":{"$orderby":{"OriginatingAgency":1}},"$projection":{}}};
            var accessionRegisterSummary = [{
              "_id": "aefaaaaaaaaam7mxaabdqakyue3nlzaaaaaq",
              "_tenant": 0,
              "OriginatingAgency": "FRAN_NP_005568",
              "TotalObjects": {
                "Total": 30,
                "Deleted": 0,
                "Remained": 30
              },
              "TotalObjectGroups": {
                "Total": 12,
                "Deleted": 0,
                "Remained": 12
              },
              "TotalUnits": {
                "Total": 36,
                "Deleted": 0,
                "Remained": 36
              },
              "ObjectSize": {
                "Total": 3427596,
                "Deleted": 0,
                "Remained": 3427596
              },
              "creationDate": "2016-11-26T15:17:55.300"
            }];
            var accessionRegisterDetail = [{
              "_id": "aedqaaaaacaam7mxaadfaakyue3k5iaaaaaq",
              "_tenant": 0,
              "OriginatingAgency": "FRAN_NP_005568",
              "SubmissionAgency": "FRAN_NP_005061",
              "EndDate": "2016-11-26T16:17:55.529+01:00",
              "StartDate": "2016-11-26T16:17:55.530+01:00",
              "Status": "STORED_AND_COMPLETED",
              "TotalObjectGroups": {
                "total": 1,
                "deleted": 0,
                "remained": 1
              },
              "TotalUnits": {
                "total": 5,
                "deleted": 0,
                "remained": 5
              },
              "TotalObjects": {
                "total": 1,
                "deleted": 0,
                "remained": 1
              },
              "ObjectSize": {
                "total": 226224,
                "deleted": 0,
                "remained": 226224
              }
            }, {
              "_id": "aedqaaaaacaam7mxaadfaakyf2k16jaaaaaq",
              "_tenant": 0,
              "OriginatingAgency": "FRAN_NP_005568",
              "SubmissionAgency": "FRAN_NP_005062",
              "EndDate": "2016-11-26T16:17:55.529+01:00",
              "StartDate": "2016-11-26T16:17:55.530+01:00",
              "Status": "STORED_AND_COMPLETED",
              "TotalObjectGroups": {
                "total": 5,
                "deleted": 0,
                "remained": 5
              },
              "TotalUnits": {
                "total": 6,
                "deleted": 0,
                "remained": 6
              },
              "TotalObjects": {
                "total": 2,
                "deleted": 0,
                "remained": 2
              },
              "ObjectSize": {
                "total": 2260224,
                "deleted": 0,
                "remained": 2260224
              }
            }];

            // Mock login http call
            $httpBackend.whenPOST(/ihm-demo\/v1\/api\/login/)
              .respond(200, {});

            $httpBackend.whenPOST(/ihm-demo\/v1\/api\/admin\/accession-register/)
              .respond(function(type, uri, data) {
                var json = JSON.parse(data);
                var response = responseEncapsulation;
                if (!!json.ACCESSIONREGISTER && json.ACCESSIONREGISTER === 'ACCESSIONREGISTER') {
                  // Mock first getSummary call without args
                  response.$results = accessionRegisterSummary;
                  console.log('Return ', response);
                } else if (json.OriginatingAgency === 'UNKNOW_ORIGATING_AGENCY_FOR_TEST') {
                  // Mock second getSummary call with BAD_ARG
                  response = responseEncapsulation;
                  response.$hits.total = 0;
                  response.$hits.size = 0;
                  response.$results = [];
                  console.log('Return ', response);
                } else {
                  // Mock third getSummary call with GOOD_ARG
                  response = responseEncapsulation;
                  response.$results = accessionRegisterSummary;
                  response.$results[0].OriginatingAgency = json.OriginatingAgency;
                  console.log('Return ', response);
                }
                return [200, response];
              });


            // Mock getDetails
            $httpBackend.whenPOST(/accession-register-detail/)
              .respond(function() {
                console.log('Get details');
                var response = responseEncapsulation;
                response.$results = accessionRegisterDetail;
                console.log('Return ', response);
                return [200, response];
              });

            // Mock logbook messages
            $httpBackend.whenGET(/ihm-demo\/v1\/api\/messages\/logbook/).respond(200, {});

            // Ignore static resources call (html/json)
            $httpBackend.whenGET(/views\/login.html/).passThrough();
            $httpBackend.whenGET(/views\/upload-sip.html/).passThrough();
            $httpBackend.whenGET(/accession-register-search.template.html/).passThrough();
            $httpBackend.whenGET(/accession-register-details.template.html/).passThrough();
            $httpBackend.whenGET(/\.json$/).passThrough();
          })
      });
    }

    logInlogOutUtilsService.doLogin(browser, element, by);
  });

  afterAll(function() {
    logInlogOutUtilsService.doLogout(element, by);
  });

  it('should go to the accession register search', function () {
    browser.get(browser.baseUrl + '/');

    // Get search link and launch search
    var navBar = element(by.id('navbar'));
    var ul = navBar.element(by.css('[class="nav navbar-nav"]'));
    // FIXME : Get the 1st index menu because by.css('[class="@@mainMenu2 dropdown"]') don't work.
    var li = ul.all(by.css('.dropdown')).get(1);
    li.getAttribute('class').then(function (value) {
      expect(value).toBe('@@mainMenu2 dropdown');
    });

    // Open the dropbox and click the search link
    li.element(by.css('[class="block dropdown-toggle"]')).click();
    li.element(by.css('[href="#!/accessionRegister/search"]')).click();
    expect(browser.getCurrentUrl()).toMatch(/.*\/accessionRegister\/search/);
  });

  it('should search automatically and return all result', function() {
    // FIXME Be sure that at least one result is present when tests are launched (for non-mocking mode)
    browser.get(browser.baseUrl + '/accessionRegister/search');
    expect(browser.getCurrentUrl()).toMatch(/.*\/accessionRegister\/search/);

    // Check automatic search is launched
    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));

    expect(trs.count()).not.toEqual(0);
    var firstResult = trs.first();
    expect(firstResult.isPresent()).toBeTruthy();
    var firstColumn = firstResult.all(by.css('td')).first();
    firstColumn.getText().then(function(value) {
      accessionRegisterId = value;

      // Click on a result and check the destination location
      firstResult.click().then(function () {
        browser.getAllWindowHandles().then(function (handles) {
          var newWindowHandle = handles[1]; // this is your new window
          browser.switchTo().window(newWindowHandle).then(function () {
            var regex = new RegExp(".*\/accessionRegister\/detail\/" + accessionRegisterId, "");
            expect(browser.getCurrentUrl()).toMatch(regex);
            browser.close();
            browser.switchTo().window(handles[0]);
          });
        });
      });
    });
  });

  it('shouldn\'t throw result for wrong parameters', function() {
    browser.get(browser.baseUrl + '/accessionRegister/search');
    expect(browser.getCurrentUrl()).toMatch(/.*\/accessionRegister\/search/);
    // TODO Be sure that this keyword is not used for any Origating Agency (for non-mocking mode)
    var keyword = 'UNKNOW_ORIGATING_AGENCY_FOR_TEST';
    element(by.model('serviceProducerCriteria')).sendKeys(keyword);
    element(by.css('[type="submit"]')).click();

    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));
    // Check only 1 result is present
    expect(trs.count()).toBe(0);
    var searchResults = element(by.css('[data-target="#boxSearchResults"]'));
    var searchString = searchResults.element(by.css('h2')).element(by.css('span'));
    expect(searchString.getText()).toBe('(0)');
  });

  it('should search a specific result', function() {
    browser.get(browser.baseUrl + '/accessionRegister/search');
    expect(browser.getCurrentUrl()).toMatch(/.*\/accessionRegister\/search/);
    // FIXME Update (dynamic ?) keyword for a correspondance with an ingested SIP (for non-mocking mode) and with unique result
    var keyword = 'OAIFERNANDES';
    element(by.model('serviceProducerCriteria')).sendKeys(keyword);
    element(by.css('[type="submit"]')).click();

    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));
    // Check only 1 result is present
    expect(trs.count()).toBe(1);
    var firstResult = trs.first();
    expect(firstResult.isPresent()).toBeTruthy();
    var firstColumn = firstResult.all(by.css('td')).first();
    firstColumn.getText().then(function(value) {
      accessionRegisterId = value;
      expect(value).toBe(keyword);

      // Click on a result and check the destination location
      firstResult.click().then(function () {
        browser.getAllWindowHandles().then(function (handles) {
          var newWindowHandle = handles[1]; // this is your new window
          browser.switchTo().window(newWindowHandle).then(function () {
            var regex = new RegExp(".*\/accessionRegister\/detail\/" + accessionRegisterId, "");
            expect(browser.getCurrentUrl()).toMatch(regex);
            browser.close();
            browser.switchTo().window(handles[0]);
          });
        });
      });
    });
  });

  it('should update search when press ENTER key', function() {
    browser.get(browser.baseUrl + '/accessionRegister/search');
    expect(browser.getCurrentUrl()).toMatch(/.*\/accessionRegister\/search/);
    // FIXME Update (dynamic ?) keyword for a correspondance with an ingested SIP (for non-mocking mode) and with unique result
    var keyword = 'OAIFERNANDES';
    element(by.model('serviceProducerCriteria')).sendKeys(keyword);
    element(by.model('serviceProducerCriteria')).sendKeys('\n');

    var table = element(by.css('[class="table"]'));
    var trs = table.all(by.css('[role="button"]'));
    // Check only 1 result is present
    expect(trs.count()).toBe(1);
    var searchResults = element(by.css('[data-target="#boxSearchResults"]'));
    var searchString = searchResults.element(by.css('h2')).element(by.css('span'));
    expect(searchString.getText()).toBe('(1)');
  });

  it('should display the good information about result', function() {
    browser.get(browser.baseUrl + '/accessionRegister/detail/' + accessionRegisterId);
    var regex = new RegExp(".*\/accessionRegister\/detail\/" + accessionRegisterId, "");
    expect(browser.getCurrentUrl()).toMatch(regex);

    var titleSpans = element.all(by.css('[class="panel-header no-toggle"]'));
    var h2Title = titleSpans.get(0).element(by.css('h2'));
    expect(h2Title.getText()).toBe('SERVICE PRODUCTEUR - ' + ('' + accessionRegisterId).toUpperCase());
  });

  it('should browse to good pages with accession-registry breadcrumb', function() {
    browser.get(browser.baseUrl + '/accessionRegister/detail/' + accessionRegisterId);
    var regex = new RegExp(".*\/accessionRegister\/detail\/" + accessionRegisterId, "");
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