import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogbookOperationComponent } from './logbook-operation.component';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { ArchiveUnitHelper } from "../../archive-unit/archive-unit.helper";
import { LogbookService } from "../../ingest/logbook.service";
import { VitamResponse } from "../../common/utils/response";
import { Observable } from "rxjs";

const LogbookServiceStub = {
  getResults: () => Observable.of(new VitamResponse())
};

describe('LogbookOperationComponent', () => {
  let component: LogbookOperationComponent;
  let fixture: ComponentFixture<LogbookOperationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationComponent ],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: LogbookService, useValue: LogbookServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookOperationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
