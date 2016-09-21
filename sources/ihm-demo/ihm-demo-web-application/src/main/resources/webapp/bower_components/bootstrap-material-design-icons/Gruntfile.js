/* jshint nod: true */
"use strict";

var livereload = {
    host: 'localhost',
    port: 35729,
};

module.exports = function(grunt) {
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        sass: {
            dist: {
                options: {
                    sourcemap: 'none',
                    unixNewlines: true,
                    compass: true,
                    lineNumbers: false,
                },
                files: {
                    'css/material-icons.css' : 'scss/material-icons.scss'
                }
            },
            demo: {
                options: {
                    sourcemap: 'none',
                    unixNewlines: true,
                    compass: true,
                    lineNumbers: true,
                },
                files: {
                    'demo/style/main.css' : 'demo/style/main.scss'
                }
            },
        },
        cssmin: {
            options: {
                sourceMap: true
            },
            target: {
                files: {
                    'css/material-icons.min.css': ['css/material-icons.css']
                }
            }
        },
        watch: {
            css: {
                files: [
                    'scss/*.scss',
                    'demo/style/*.scss'
                ],
                tasks: ['sass'],
                options: {
                    livereload: livereload
                }
            }
        }
    });
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.registerTask('default', ['sass', 'watch']);
}