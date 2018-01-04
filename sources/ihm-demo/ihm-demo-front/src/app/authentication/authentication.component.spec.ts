import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { PanelModule, DropdownModule, InputTextModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule }   from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { AuthenticationComponent } from './authentication.component';
import { AuthenticationService } from './authentication.service';
import {TranslateService} from "@ngx-translate/core";

const AuthenticationServiceStub = {
  verifyAuthentication: (username : string, password : string) => {
    return Observable.of('');
  },
  getTenants : () => {
    return Observable.of([0, 1, 2]);
  },
  getAuthenticationMode: () => {
    return Observable.of(['ldap', 'ini']);
  },
  isLoggedIn : () => false,
  loggedOut : () => {}
};

describe('AuthenticationComponent', () => {
  let component: AuthenticationComponent;
  let fixture: ComponentFixture<AuthenticationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AuthenticationComponent ],
      imports: [ PanelModule, DropdownModule, InputTextModule, FormsModule, BrowserAnimationsModule, RouterTestingModule, ReactiveFormsModule],
      providers: [
        {provide: AuthenticationService, useValue : AuthenticationServiceStub},
        {provide: TranslateService, useValue: translateServiceStub}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  let translateServiceStub = {
    reloadLang: (string) => Observable.of({}),
    getDefaultLang: () => 'fr'
  };

  beforeEach(() => {
    fixture = TestBed.createComponent(AuthenticationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
