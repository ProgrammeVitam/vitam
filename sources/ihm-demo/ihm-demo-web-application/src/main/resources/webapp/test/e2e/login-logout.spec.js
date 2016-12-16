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

describe('login logout navigatiion', function() {
  var genericUtilsService = require('./utils/generic-utils.function');

  beforeEach(function() {
    if(browser.params.mock === true) {
      browser.addMockModule('httpMocker', function () {
        angular.module('httpMocker', ['ngMockE2E'])
          .run(function ($httpBackend) {

            // Mock login http call
            $httpBackend.whenPOST(/ihm-demo\/v1\/api\/login/)
              .respond(200, {});

            // Ignore static resources call (html/json)
            $httpBackend.whenGET(/views\/login.html/).passThrough();
            $httpBackend.whenGET(/views\/upload-sip.html/).passThrough();
            $httpBackend.whenGET(/\.json$/).passThrough();
          })
      });
    }
  });

  it('should login correctly', function() {
    var doTheJob = function() {
      expect(browser.getCurrentUrl()).toMatch(/.*\/login/);
      genericUtilsService.checkNoBreadcrumb(element, by, expect);
      var loggedMenu = element(by.id('navbar'));
      expect(loggedMenu.isPresent()).toBeFalsy();

      element(by.model('credentials.username')).sendKeys(browser.params.userName);
      element(by.model('credentials.password')).sendKeys(browser.params.password);
      element(by.css('[type="submit"]')).click();
      expect(browser.getCurrentUrl()).toMatch(/.*\/uploadSIP$/);
      genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 1, 'Transfert');

      loggedMenu = element(by.id('navbar'));
      expect(loggedMenu.isPresent()).toBeTruthy();
    };

    browser.get(browser.baseUrl + '/login').then(doTheJob);
  });

  it('should logout correctly', function() {
    browser.get(browser.baseUrl + '/uploadSIP');
    var navBar = element(by.id('navbar'));

    // Check the page and the connected mode (menu present)
    expect(browser.getCurrentUrl()).toMatch(/.*\/uploadSIP$/);
    genericUtilsService.checkBreadcrumbFinalPart(element, by, expect, 1, 'Transfert');
    expect(navBar.isPresent()).toBeTruthy();

    // Get the Settings Button and check that menu is not visible
    var rightDiv = navBar.element(by.css('[class="nav navbar-nav navbar-right"]'));
    expect(rightDiv.isPresent()).toBeTruthy();
    var settingButton = rightDiv.element(by.css('[class="block dropdown-toggle"]'));
    expect(settingButton.isPresent()).toBeTruthy();
    settingButton.getAttribute("aria-expanded").then(function (value) {
      expect(value).toBeFalsy();
    });

    // Chick the button and check menu is visible
    settingButton.click();
    settingButton.getAttribute("aria-expanded").then(function (value) {
      expect(value).toBeTruthy();
    });

    // Get logout button and click on in then check disconnected state and url
    var logOutButton = rightDiv.element(by.css('[ng-click="logoutUser()"]'));
    logOutButton.click();
    expect(browser.getCurrentUrl()).toMatch(/.*\/login/);
    genericUtilsService.checkNoBreadcrumb(element, by, expect);
    expect(navBar.isPresent()).toBeFalsy();
  });

});