import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { BreadcrumbService } from "../../common/breadcrumb.service";
import { AuditComponent } from './audit.component';
import { ReferentialsService } from "../../referentials/referentials.service";
import { AuditService } from "./audit.service";

const ReferentialsServiceStub = {
  getResults: (id) => Observable.of({'$results': [{}]}),
  setSearchAPI : (id) => {},
  getTenantCurrent : () => 0
};

describe('AuditComponent', () => {
  let component: AuditComponent;
  let fixture: ComponentFixture<AuditComponent>;



  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: AuditService, useValue: ReferentialsServiceStub }
      ],
      declarations: [ AuditComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AuditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
