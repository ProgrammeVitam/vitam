# Front

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 1.1.2.

## Development server

Run `npm run start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

A proxy is added on that script in order to redirect /ihm-recette requests to the backend server in proxy.conf.json.
You can copy the proxy.conf.json.sample to proxy.conf.json in order to make the build work.
Update target with folowing properties:
-For http backend:
"host": ${your_host},
"protocol": "http:",
"port": 80
-For https backend:
"host": ${your_host},
"protocol": "https:",
"port": 443

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|module`.

## Build

Run `npm run build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `-prod` flag for a production build.

## Running unit tests

Run `npm run test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running lint

Run `npm run lint` to execute format and style code checks. use tslint.json file.

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).
Before running the tests make sure you are serving the app via `ng serve`.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).

## Global theme update

In order to update the global css (theme) you should process as:
- Update theme in 'ihm-recette-front/themes/vitam-red/theme.scss'
- Generate css with the command line 'sass themes/vitam-red/theme.scss:src/styles.css'
- Copy generated styles.css in 'ihm-recette-front/front/src' folder (Or remove styles.css.map if auto link to src folder with sass)

