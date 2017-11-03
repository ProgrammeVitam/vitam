import {Injectable} from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import 'rxjs/add/operator/map';
import {ResourcesService} from '../common/resources.service';
import {BehaviorSubject} from "rxjs/BehaviorSubject"
import {Observable} from "rxjs/Observable";
import {Response} from "@angular/http";
import {TenantService} from "../common/tenant.service";


const LOGGED_IN = 'loggedIn';

@Injectable()
export class AuthenticationService {

  constructor(private resourceService: ResourcesService, private cookies: CookieService, private tenantService: TenantService, private resourcesService: ResourcesService) {
  }

  getSecureMode() {
    return this.resourceService.get('securemode').map((res: Response) => res.text());
  }

  verifyAuthentication() {
    return this.resourceService.getTenants();
  }

  private loginState = new BehaviorSubject<boolean>(false);

  loggedIn() {
    this.cookies.put(LOGGED_IN, 'true');
    this.loginState.next(true);
  }

  logIn(id: string, password: string) {
    this.resourceService.setTenant("");
    this.tenantService.changeState(this.resourcesService.getTenant());
    return this.resourceService.post('login', null, {
      "token": {"principal": id, "credentials": password}
    });
  }

  loggedOut() {
    this.resourceService.setTenant("");
    this.tenantService.changeState(this.resourcesService.getTenant());
    this.cookies.put(LOGGED_IN, 'true');
    this.loginState.next(false);
  }

  logOut() {
    return this.resourceService.post('logout', null, {});
  }

  getState(): BehaviorSubject<boolean> {
    return this.loginState;
  }
}
