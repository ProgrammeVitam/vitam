import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { LogbookComponent } from './logbook.component';
import { LogbookService } from '../logbook.service';
import {VitamResponse} from "../../common/utils/response";
import { IngestUtilsService } from '../../common/utils/ingest-utils.service';
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { ArchiveUnitHelper } from "../../archive-unit/archive-unit.helper";

const LogbookServiceStub = {
  getResults: () => Observable.of(new VitamResponse())
};

const IngestUtilsServiceStub = {

};

describe('LogbookComponent', () => {
  let component: LogbookComponent;
  let fixture: ComponentFixture<LogbookComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookComponent ],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: IngestUtilsService, useValue: IngestUtilsServiceStub },
        { provide: LogbookService, useValue: LogbookServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
