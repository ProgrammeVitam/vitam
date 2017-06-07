'use strict';

var pack = require('./package.json');
var gulp = require('gulp');
var bower = require('gulp-bower');
var concat = require('gulp-concat');
var gulpsync = require('gulp-sync')(gulp);
var del = require('del');
var zip = require('gulp-zip');
var gulpif = require('gulp-if');
var minifyHtml = require("gulp-minify-html");
var connect = require('gulp-connect');
var proxy = require('http-proxy-middleware');
var jshint = require('gulp-jshint');
var cleanCSS = require('gulp-clean-css');
var minifyJS = require("gulp-uglify");
var ngAnnotate = require('gulp-ng-annotate');
var Server = require('karma').Server;
var karma = require('karma');
var angularProtractor = require('gulp-angular-protractor');

var pomVersion = process.env.pomVersion;


var production = true;

var config = {
    assets: [
        '/bower_components/bootstrap-material-design-icons/fonts/*'
    ],
    sourceDirectory: 'app',
    testDirectory: 'test',
    targetDirectory: 'dist'
};

gulp.task('minifyHtml', function () {
    gulp.src('./**/*.html')
        .pipe(minifyHtml())
        .pipe(gulp.dest(config.targetDirectory));
});

gulp.task('package', gulpsync.sync(['bower', /*'lint';*/ 'build']), function () {
    var name = pack.name + '-' + pomVersion + '.zip';
    return gulp.src([config.targetDirectory + '/**/*'])
        .pipe(zip(name))
        .pipe(gulp.dest('./target/'));
});

gulp.task('clean', function () {
    return del([config.targetDirectory, 'vitam-*.zip', 'bower_components']);
});

gulp.task('bower', function () {
    gulp.src('./bower_components/**/*')
        .pipe(gulp.dest(config.targetDirectory + '/vendor'));
});

gulp.task('build:css', function () {
    return gulp.src(config.sourceDirectory + '/css/*.css')
        .pipe(gulpif(production, cleanCSS()))
        .pipe(gulp.dest(config.targetDirectory + '/css'));
});

gulp.task('build:html', function () {
    return gulp.src([config.sourceDirectory + '/**/*.html', config.sourceDirectory + '/index.html', '!' + config.sourceDirectory + '/node_modules/**'])
        .pipe(gulpif(production, minifyHtml()))
        .pipe(gulp.dest(config.targetDirectory));
});

gulp.task('build:js', ['bower'], function () {
    return gulp.src(config.sourceDirectory + '/**/*.js')
        .pipe(ngAnnotate())
        .pipe(gulpif(production, minifyJS()))
        .pipe(gulp.dest(config.targetDirectory));
});

gulp.task('build:assets', function () {
    return gulp.src(
        [config.sourceDirectory + '/images/*', config.sourceDirectory + '/static/*', config.sourceDirectory + '/css/fonts/*'],
        {base: config.sourceDirectory}
    ).pipe(gulp.dest(config.targetDirectory))
});

gulp.task('build:vendor-assets', function () {
    return gulp.src(config.sourceDirectory + '/bower_components/bootstrap-material-design-icons/fonts/*')
        .pipe(gulp.dest(config.targetDirectory + '/bower_components/bootstrap-material-design-icons/fonts/'));
});

gulp.task('vendor:css', function () {
    return gulp.src('./bower_components/**/*.css')
        .pipe(gulp.dest(config.targetDirectory + '/vendor'));
});


gulp.task('build', gulpsync.sync(['build:css', 'build:assets', 'build:vendor-assets', 'build:html', 'vendor:css', 'build:js', 'bower']), function () {
});

gulp.task('default', ['serve']);

function serve() {
  var target = 'http://localhost:8082';
  var port = 9002;

  try {
    var customConf = require('./local.json');
    if(!!customConf && !!customConf.target) {
      target = customConf.target;
    }
    if(!!customConf && !!customConf.port) {
      port = customConf.port;
    }
  } catch (e) {
    // File not present / Just dont override conf
  }

  connect.server({
    root: ['dist/'],
    port: port,
    livereload: true,
    middleware: function (connect, opt) {
      return [
        proxy('/ihm-recette', {
          target: target,
          changeOrigin: true
        })
      ]
    }
  });
}

gulp.task('serve', ['watch'], function () {
    production = false;
    serve();
});

gulp.task('watch', ['build'], function () {
    gulp.watch(config.sourceDirectory + '/**/*.js', ['reload:js']);
    gulp.watch(config.sourceDirectory + '/**/*.html', ['reload:html']);
    gulp.watch(config.sourceDirectory + '/**/*.css', ['reload:css']);
});

gulp.task('reload', ['reload:css', 'reload:js', 'reload:html']);

gulp.task('reload:js', ['build:js'], function () {
    return gulp.src(config.targetDirectory + '/**/*.js')
        .pipe(connect.reload());
});

gulp.task('reload:html', ['build:html'/*, 'build:index'*/], function () {
    return gulp.src(config.targetDirectory + '/**/*.html')
        .pipe(connect.reload());
});

gulp.task('reload:css', ['build:css'], function () {
    return gulp.src(config.sourceDirectory + '/**/*.css')
        .pipe(connect.reload());
});

gulp.task('lint', function () {
    return gulp.src(config.sourceDirectory + '/**/*.js')
        .pipe(jshint())
        .pipe(jshint.reporter('jshint-stylish'))
        .pipe(jshint.reporter('fail'));
});

// TODO: add build on next step. If needed only launch karma for integration
// TODO: Launch a server for e2e tests via serve task
gulp.task('tests', gulpsync.sync(['serve', 'testKarma', 'testProtractor']));

gulp.task('testKarma', function (cb) {
  new Server.start({
    configFile: __dirname + '/karma.conf.js',
    singleRun: true
  }, function () {
    cb()
  });
});

gulp.task('testProtractor', function () {
  var conf = {
    'configFile': 'protractor.conf.js',
    'autoStartStopServer': true,
    'debug': true
  };

  gulp.src(['./test/e2e/**/*.js'])
    .pipe(angularProtractor(conf))
    .on('error', function(e) {
      console.log('Erorr: ', e);
      throw e
    });
});
