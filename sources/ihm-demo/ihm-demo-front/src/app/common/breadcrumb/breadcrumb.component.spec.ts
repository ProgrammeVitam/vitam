import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {BreadcrumbModule, DialogModule, FieldsetModule, PanelModule} from 'primeng/primeng';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {Observable} from 'rxjs/Observable';
import { BehaviorSubject } from "rxjs/BehaviorSubject"
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { BreadcrumbComponent } from './breadcrumb.component';
import {BreadcrumbElement, BreadcrumbService} from '../breadcrumb.service';
import { AuthenticationService } from '../../authentication/authentication.service';

let value: BreadcrumbElement[];
const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};

const AuthenticationServiceStub = {
  getLoginState: () => new BehaviorSubject<boolean>(false)
};

describe('BreadcrumbComponent', () => {
  let component: BreadcrumbComponent;
  let fixture: ComponentFixture<BreadcrumbComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BreadcrumbComponent ],
      imports: [BreadcrumbModule, PanelModule, BrowserAnimationsModule],
      providers: [
        {provide: BreadcrumbService, useValue: BreadcrumbServiceStub},
        {provide: AuthenticationService, useValue : AuthenticationServiceStub}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BreadcrumbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
