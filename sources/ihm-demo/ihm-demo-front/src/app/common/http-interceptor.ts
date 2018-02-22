import {Injectable} from '@angular/core';
import {HttpEvent, HttpInterceptor, HttpHandler, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import { Router } from '@angular/router';
import {CookieService} from 'angular2-cookie/core';
import {DialogService} from './dialog/dialog.service';

const USER = 'user';
const LOGGED_IN = 'loggedIn';
const DEFAULT_TIMEOUT = 1800000;

@Injectable()
export class VitamInterceptor implements HttpInterceptor {

  sessionTimeout: any;

  constructor(private cookies: CookieService, private router: Router,
              private dialogService: DialogService) {
    window.addEventListener('storage', (event) => {
      if (event.key === 'reset-timeout') {
        this.restartLoginTimeOut();
        localStorage.removeItem('reset-timeout');
      }
    });
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    localStorage.setItem('reset-timeout', Math.random().toString());
    this.restartLoginTimeOut();

    return next.handle(req).catch(
      (error) => {
        // Handle 403 errors
        if (error.status === 403) {
          this.logoutUser();
          this.dialogService.displayMessage(
            'Vous avez été déconnecté. Merci de vous authentifier',
            'Accès interdit'
          );
        }

        return Observable.throw(error);
      });
  }

  restartLoginTimeOut() {
    if (localStorage.getItem(USER)) {
      const userInformation = JSON.parse(localStorage.getItem(USER));
      if (this.sessionTimeout) {
        clearTimeout(this.sessionTimeout);
      }
      if (userInformation) {
        this.sessionTimeout = setTimeout(() => {
          this.logoutUser();
        }, userInformation.sessionTimeout || DEFAULT_TIMEOUT);
      }
    }
  }
  logoutUser() {
    localStorage.setItem(LOGGED_IN, 'false');
    localStorage.removeItem(USER);
    this.router.navigate(['login']);
  }
}
