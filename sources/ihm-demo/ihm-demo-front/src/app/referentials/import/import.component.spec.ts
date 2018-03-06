import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { ImportComponent } from './import.component';
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { AuthenticationService } from '../../authentication/authentication.service';

const AuthenticationServiceStub = {
  isTenantAdmin: () => {return true;}
};

describe('ImportComponent', () => {
  let component: ImportComponent;
  let fixture: ComponentFixture<ImportComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([
          { path: 'ingest/sip', component: ImportComponent }
        ])
      ],
      providers: [ BreadcrumbService,
        { provide: AuthenticationService, useValue: AuthenticationServiceStub } ],
      declarations: [ ImportComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ImportComponent);
    component = fixture.componentInstance;
    component.referentialType = 'accessContract';
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
