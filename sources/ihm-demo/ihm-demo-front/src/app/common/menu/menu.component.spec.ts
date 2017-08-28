import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import {ButtonModule, GrowlModule, MenubarModule} from 'primeng/primeng';
import {RouterTestingModule} from '@angular/router/testing';
import {FormsModule} from '@angular/forms';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { MenuComponent } from './menu.component';
import {ResourcesService} from '../resources.service';
import { AuthenticationService } from '../../authentication/authentication.service';
import { AccessContractService } from '../access-contract.service';

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
  getLoginState: () => Observable.of(''),
  logIn: () => Observable.of({ status : 200 }),
  loggedIn: () => {},
  loggedOut: () => {},
  logOut: () => Observable.of('')
};

describe('MenuComponent', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MenuComponent ],
      imports: [
        MenubarModule,
        ButtonModule,
        RouterTestingModule.withRoutes([
          { path: 'login', component: MenuComponent }
        ]),
        GrowlModule,
        FormsModule
      ],
      providers: [
        AccessContractService,
        { provide: ResourcesService, useValue: ResourcesServiceStub },
        { provide: AuthenticationService, useValue: AuthenticationServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
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
