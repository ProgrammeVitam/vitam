import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { LogbookDetailsComponent } from './logbook-details.component';
import { Observable } from "rxjs";
import { ActivatedRoute } from "@angular/router";
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { VitamResponse } from "../../../common/utils/response";
import { LogbookService } from "../../logbook.service";
import { ArchiveUnitHelper } from "../../../archive-unit/archive-unit.helper";

const LogbookServiceStub = {
  getDetails: (id) => Observable.of({$results: [{events: []}]})
};

describe('LogbookDetailsComponent', () => {
  let component: LogbookDetailsComponent;
  let fixture: ComponentFixture<LogbookDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookDetailsComponent ],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: LogbookService, useValue: LogbookServiceStub },
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 1})} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
