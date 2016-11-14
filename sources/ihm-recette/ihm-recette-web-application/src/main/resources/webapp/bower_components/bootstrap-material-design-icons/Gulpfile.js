/* jshint node: true */
"use strict";

var _ = require('lodash'),
    fs = require('fs'),
    gulp = require('gulp'),
    gutil = require('gulp-util'),
    changed = require('gulp-changed'),
    File = require('vinyl'),
    path = require('path'),
    through2 = require('through2');


var sassClassPrefix = 'md-css-prefix',
    modulePath = './node_modules/material-design-icons',
    srcCodepoints = modulePath + '/iconfont/codepoints',
    fontsPath = './fonts',
    dstCodepoints = fontsPath + '/codepoints',
    sassFile = 'scss/_icons.scss',
    sassHeader = '@import "variables";\n\n',
    demoDataFile = 'demo/js/data.js'


function generateSassCodepoints(filepath) {
    return through2.obj(function(codepointsFile, encoding, callback) {
        function codepointsToSass(codepoints) {
            return _(codepoints)
                .split('\n')
                .reject(_.isEmpty)
                .reduce(function(file, line) {
                    let codepoint = line.split(' ');
                    file += '.#{$' + sassClassPrefix + '}-' + codepoint[0].replace(/_/g, '-') +
                        ':before { content: "\\' + codepoint[1]  + '"; }\n';
                    return file;
                }, sassHeader);
        }
        callback(null, new File({
            path: filepath,
            contents: new Buffer(codepointsToSass(codepointsFile.contents), 'utf8')
        }));
    });
}


gulp.task('update-sass', function() {
    gulp.src(srcCodepoints)
        .pipe(changed(fontsPath, {hasChanged: changed.compareSha1Digest}))
        .pipe(generateSassCodepoints(sassFile))
        .pipe(gulp.dest('.'));
});

function generateDataCodepoints(filepath) {
    return through2.obj(function(codepointsFile, encoding, callback) {
        var countIcons = 0,
            newIcons = 0;

        function codepoints2obj(codepoints) {
            return _(codepoints)
                .split('\n')
                .reject(_.isEmpty)
                .reduce(function(obj, line) {
                    let codepoint = line.split(' ');
                    obj[codepoint[0]] = codepoint[1];
                    return obj;
                }, {});
        }

        function scanCategories(dir, codes) {
            var categories = {};
            fs.readdirSync(dir)
            .filter(function (file) {
                return fs.statSync(path.join(dir, file)).isDirectory()
                    && fs.existsSync(path.join(dir, file, "svg/production"));
            })
            .forEach(function (category) {
                categories[category] = {};
                let catPath = path.join(dir, category, "svg/production");
                fs.readdirSync(catPath)
                .filter(function(file) {
                    return file.match(/^ic_(.+?)_\d+px\.svg$/);
                })
                .forEach(function(file) {
                    let matches;
                    if (matches = /^ic_(.+?)_\d+px\.svg$/.exec(file)) {
                        let icon = matches[1];
                        if (codes[icon]) {
                            if (!categories[category][icon]) {
                                categories[category][icon] = codes[icon];
                                countIcons ++;
                            }
                        }
                    }
                });
            });
            return categories;
        }

        function calculateNewIcons(categories, codes, old) {
            var cats = categories;
            Object.keys(categories).forEach(function(name) {
                let category = categories[name];
                Object.keys(category).forEach(function(icon) {
                    let code = category[icon];
                    cats[name][icon] = [code, !old[icon]];
                    if (!old[icon]) {
                        newIcons ++;
                    }
                });
            });
            return cats;
        }

        var codes = codepoints2obj(codepointsFile.contents);
        var categories = scanCategories(modulePath, codes);
        categories = calculateNewIcons(categories, codes,
            codepoints2obj(fs.readFileSync(dstCodepoints).toString()));

        gutil.log('Was found', gutil.colors.red(newIcons), 'new icons');
        gutil.log('Total found', gutil.colors.red(countIcons), 'icons.');

        callback(null, new File({
            path: filepath,
            contents: new Buffer(
                'window.data = ' + JSON.stringify(categories) + ';', 'utf8')
        }));
    });
}

gulp.task('update-demo-data', function() {
    gulp.src(srcCodepoints)
        .pipe(changed(fontsPath, {hasChanged: changed.compareSha1Digest}))
        .pipe(generateDataCodepoints(demoDataFile))
        .pipe(gulp.dest('.'));
});

gulp.task('update-codepoints', function() {
    gulp.src(srcCodepoints)
        .pipe(gulp.dest(fontsPath));
})

gulp.task('default', ['update-sass', 'update-demo-data']);
