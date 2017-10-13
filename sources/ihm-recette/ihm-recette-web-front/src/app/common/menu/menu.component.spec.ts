import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MenuComponent } from './menu.component';
import {ButtonModule, GrowlModule, MenubarModule} from 'primeng/primeng';
import {RouterTestingModule} from '@angular/router/testing';
import {FormsModule} from '@angular/forms';
import {ResourcesService} from '../resources.service';
import {Observable} from 'rxjs/Observable';

import { AuthenticationService } from '../../authentication/authentication.service';
import {TenantService} from "../tenant.service";

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  delete: (url) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};
const AuthenticationServiceStub = {
  getSecureMode: () => Observable.of('x509'),
  verifyAuthentication: () => Observable.of(''),
  logIn: () => Observable.of({ status : 200 }),
  loggedIn: () => {},
  loggedOut: () => {},
  getState: () => Observable.of(true)
};
const TenantServiceStub = {
  getState: () => Observable.of('0')
};

describe('MenuComponent', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MenuComponent ],
      imports: [ MenubarModule, ButtonModule, RouterTestingModule, GrowlModule, FormsModule ],
      providers: [
        { provide: ResourcesService, useValue: ResourcesServiceStub },
        { provide: AuthenticationService, useValue: AuthenticationServiceStub },
        { provide: TenantService, useValue: TenantServiceStub }
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
