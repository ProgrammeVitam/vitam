exports.config = {
  framework: 'jasmine2',
  seleniumAddress: 'http://127.0.0.1:4444/wd/hub',
  baseUrl: 'http://localhost:9000/#!',

  params: {
    userName: 'uuser',
    password: 'uuser1234',
    mock: true
  },

  jasmineNodeOpts: {
    defaultTimeoutInterval: 25000000
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
    // login: 'test/e2e/login-logout.spec.js'
    // accessionRegister: 'test/e2e/accession-register-search-browse.spec.js'
    // TODO : Put here other test suite
    tracabilityOperation: 'test/e2e/search-operation-browse.spec.js'
  }

};