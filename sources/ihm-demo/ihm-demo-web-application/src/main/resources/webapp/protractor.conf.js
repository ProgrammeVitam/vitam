exports.config = {
  framework: 'jasmine2',
  seleniumAddress: 'http://localhost:4444/wd/hub',
  baseUrl: 'http://localhost:8082/ihm-demo/#!',

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

  /*multiCapabilities: [{
    browserName: 'firefox'
  }, {
    browserName: 'chrome'
  }],*/

  suites: {
    // Test for login/logout
    login: 'test/e2e/login-logout.spec.js',
    // FIXME : Put here e2e tests that upload format and rules
    // functionalPreparation: '',
    // FIXME : Put here e2e tests that upload a first shot exemple SIP
    // ingestPreparation: '',
    accessionRegister: 'test/e2e/accession-register-search-browse.spec.js'
    // TODO : Put here other test suite
  }

};