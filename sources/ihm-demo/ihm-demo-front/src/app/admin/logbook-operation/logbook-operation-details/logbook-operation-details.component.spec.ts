import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FieldsetModule } from 'primeng/primeng';

import { LogbookOperationDetailsComponent } from './logbook-operation-details.component';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Observable } from "rxjs";
import { LogbookService } from "../../../ingest/logbook.service";
import { LogbookHelperService } from "../../../common/logbook-operation-events/logbook-helper.service";
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ArchiveUnitHelper } from "../../../archive-unit/archive-unit.helper";

const LogbookServiceStub = {
  getDetails: (id) => Observable.of({$results: [{events: []}]})
};
const LogbookHelperServiceStub = {

};

describe('LogbookOperationDetailsComponent', () => {
  let component: LogbookOperationDetailsComponent;
  let fixture: ComponentFixture<LogbookOperationDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationDetailsComponent ],
      providers : [
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: LogbookService, useValue: LogbookServiceStub },
        { provide: LogbookHelperService, useValue: LogbookHelperServiceStub },
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 1})} }
      ],
      imports: [ BrowserAnimationsModule, FieldsetModule ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookOperationDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
