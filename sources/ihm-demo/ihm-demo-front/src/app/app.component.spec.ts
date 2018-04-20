import {TestBed, async} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {AppComponent} from './app.component';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import {AuthenticationService} from './authentication/authentication.service';
import {TranslateLoader, TranslateModule} from '@ngx-translate/core';


const AuthenticationServiceStub = {
  getLoginState: () => Observable.of(''),
  logIn: () => Observable.of({status: 200}),
  loggedIn: () => {
  },
  loggedOut: () => {
  },
  logOut: () => Observable.of('')
};

describe('AppComponent', () => {
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AppComponent
      ],
      imports: [
        RouterTestingModule.withRoutes([
        ]),
        TranslateModule.forRoot({})
      ],
      providers: [
        {provide: AuthenticationService, useValue: AuthenticationServiceStub}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

  it(`should have as title 'vitam'`, async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app.title).toEqual('vitam');
  }));
});
