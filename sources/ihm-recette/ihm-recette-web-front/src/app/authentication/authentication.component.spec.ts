import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { BreadcrumbModule, FieldsetModule, PanelModule, InputTextModule} from 'primeng/primeng';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { FormsModule }   from '@angular/forms';
import {Observable} from 'rxjs/Observable';

import { AuthenticationComponent } from './authentication.component';
import { AuthenticationService } from './authentication.service';


const AuthenticationServiceStub = {
  getSecureMode: () => Observable.of('x509'),
  verifyAuthentication: () => Observable.of(''),
  logIn: () => Observable.of({ status : 200 }),
  loggedIn: () => {},
  loggedOut: () => {}
};

describe('AuthenticationComponent', () => {
  let component: AuthenticationComponent;
  let fixture: ComponentFixture<AuthenticationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AuthenticationComponent ],
      imports: [BreadcrumbModule, PanelModule, BrowserAnimationsModule, InputTextModule, FormsModule, RouterTestingModule],
      providers: [
        {provide: AuthenticationService, useValue: AuthenticationServiceStub}
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AuthenticationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
