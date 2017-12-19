import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { LogbookDetailsComponent } from './logbook-details.component';
import { Observable } from "rxjs";
import { ActivatedRoute } from "@angular/router";
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { LogbookService } from "../../logbook.service";
import { ArchiveUnitHelper } from "../../../archive-unit/archive-unit.helper";
import {ErrorService} from "../../../common/error.service";
import {DialogService} from "../../../common/dialog/dialog.service";
import {RouterTestingModule} from "@angular/router/testing";

const LogbookServiceStub = {
  getDetails: (id) => Observable.of({$results: [{events: []}]})
};

describe('LogbookDetailsComponent', () => {
  let component: LogbookDetailsComponent;
  let fixture: ComponentFixture<LogbookDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [ LogbookDetailsComponent ],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        ErrorService,
        { provide: LogbookService, useValue: LogbookServiceStub },
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 1})} },
        { provide: DialogService, useValue: {} }
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
