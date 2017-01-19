exports.config = {
  framework: 'jasmine2',
  seleniumAddress: 'http://127.0.0.1:4444/wd/hub',
  baseUrl: 'http://127.0.0.1:8082/ihm-demo/#!',

  params: {
    userName: 'uuser',
    password: 'uuser1234',
    mock: true
  },

  jasmineNodeOpts: {
    defaultTimeoutInterval: 2500000
  },

  capabilities: {
    'browserName': 'chrome'
  },

  /* multiCapabilities: [{
    browserName: 'firefox'
  }, {
    browserName: 'chrome'
  }], */

  suites: {
    login: 'test/e2e/login-logout.spec.js',
    // Test for login/logout
    // FIXME : Put here e2e tests that upload format and rules
    // functionalPreparation: '',
    // FIXME : Put here e2e tests that upload a first shot exemple SIP
    // ingestPreparation: '',
    accessionRegister: 'test/e2e/accession-register-search-browse.spec.js'
    // TODO : Put here other test suite
  }

};