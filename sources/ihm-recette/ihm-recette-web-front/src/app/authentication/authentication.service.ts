import {Injectable} from '@angular/core';
import {CookieService} from "angular2-cookie/core";
import 'rxjs/add/operator/map';
import {ResourcesService} from '../common/resources.service';
import {BehaviorSubject} from "rxjs/BehaviorSubject"
import {TenantService} from "../common/tenant.service";


const LOGGED_IN = 'loggedIn';

@Injectable()
export class AuthenticationService {

  private loginState : BehaviorSubject<boolean>;

  constructor(private resourceService: ResourcesService, private cookies: CookieService,
              private tenantService: TenantService, private resourcesService: ResourcesService) {

    let loggedIn = this.cookies.get(LOGGED_IN) === 'true';
    this.loginState =  new BehaviorSubject<boolean>(loggedIn);
  }

  getSecureMode() {
    return this.resourceService.get('securemode', undefined, 'text');
  }

  verifyAuthentication() {
    return this.resourceService.getTenants();
  }


  loggedIn() {
    this.cookies.put(LOGGED_IN, 'true');
    this.loginState.next(true);
  }

  logIn(id: string, password: string) {
    this.resourceService.setTenant("");
    this.tenantService.changeState(this.resourcesService.getTenant());
    return this.resourceService.post('login', null, {
      "token": {"principal": id, "credentials": password}
    }, 'text');
  }

  loggedOut() {
    this.resourceService.setTenant("");
    this.tenantService.changeState(this.resourcesService.getTenant());
    this.cookies.put(LOGGED_IN, 'false');
    this.loginState.next(false);
  }

  logOut() {
    return this.resourceService.post('logout', null, {});
  }

  getState(): BehaviorSubject<boolean> {
    return this.loginState;
  }
}
