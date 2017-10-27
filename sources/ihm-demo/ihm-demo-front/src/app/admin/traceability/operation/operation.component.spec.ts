import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable } from "rxjs";
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ArchiveUnitHelper } from "../../../archive-unit/archive-unit.helper";
import { LogbookService } from "../../../ingest/logbook.service";
import { VitamResponse } from "../../../common/utils/response";
import { OperationComponent } from './operation.component';

const LogbookServiceStub = {
  getResults: () => Observable.of(new VitamResponse())
};

describe('OperationComponent', () => {
  let component: OperationComponent;
  let fixture: ComponentFixture<OperationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OperationComponent ],
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
    fixture = TestBed.createComponent(OperationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
