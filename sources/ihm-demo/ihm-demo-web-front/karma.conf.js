module.exports = function(config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],

    files: [
      'bower_components/jquery/dist/jquery.js',
      'bower_components/lodash/dist/lodash.min.js',
      'bower_components/angular/angular.js',
      'bower_components/restangular/dist/restangular.min.js',
      'bower_components/bootstrap/dist/js/bootstrap.min.js',
      'bower_components/angular-aria/angular-aria.min.js',
      'bower_components/angular-material/angular-material.min.js',
      'bower_components/angular-mocks/angular-mocks.js',
      'bower_components/angular-resource/angular-resource.js',
      'bower_components/angular-animate/angular-animate.js',
      'bower_components/angular-cookies/angular-cookies.js',
      'bower_components/angular-translate/angular-translate.js',
      'bower_components/angular-file-upload/dist/angular-file-upload.min.js',
      'bower_components/angular-route/angular-route.js',
      'bower_components/angular-messages/angular-messages.js',
      'bower_components/angular-ui-bootstrap-bower/ui-bootstrap-tpls.js',
      'bower_components/v-accordion/dist/v-accordion.js',
      'bower_components/angular-shiro/dist/angular-shiro.js',
      'bower_components/jquery-ui/jquery-ui.js',
      'bower_components/angular-translate/angular-translate.min.js',
      'bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.min.js',
      'app/modules/parent/app.module.js',
      'app/modules/parent/app.config.js',
      'app/modules/core/core.module.js',
      'app/modules/core/ihm-demo-factory.js',
      'app/modules/core/ihm-demo-service.js',
      'app/modules/multiselect/angular-bootstrap-multiselect.js',
      'app/modules/flow/ng-flow-standalone.js',
      'app/**/*.module.js',
      'app/modules/**/*.js',
      'app/resources/*.js',
      'app/services/*.js',
      'app/pages/**/*.js',
      'test/spec/**/*.js'
    ],

    // list of files to exclude
    exclude: ['bower_compnents/**/*'],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {},


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['mocha','junit'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['Chrome'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity,

    mochaReporter: {
      output: 'noFailures'
    }
  })
};
