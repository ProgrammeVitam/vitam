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
  const okFinalEvent = {events: [{}, {evType: 'PROCESS_SIP_UNITARY', outcome: 'OK'}]};
  const koFinalEvent = {events: [{}, {evType: 'PROCESS_SIP_UNITARY', outcome: 'KO'}]};
  const runningEvent = {events: [{}, {evType: 'OBJ_STORAGE', outcome: 'OK'}]};

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

  it('should display manifest action in good status', () => {
    expect(LogbookComponent.displayManifestDownload(okFinalEvent)).toBeTruthy();
    expect(LogbookComponent.displayManifestDownload(koFinalEvent)).toBeFalsy();
    expect(LogbookComponent.displayManifestDownload(runningEvent)).toBeFalsy();
  });

  it('should display atr action in good status', () => {
    expect(LogbookComponent.displayReportDownload(okFinalEvent)).toBeTruthy();
    expect(LogbookComponent.displayReportDownload(koFinalEvent)).toBeTruthy();
    expect(LogbookComponent.displayReportDownload(runningEvent)).toBeFalsy();
  });

  it('should display good status', () => {
    expect(LogbookComponent.handleStatus(okFinalEvent.events[1])).toBe('Succès');
    expect(LogbookComponent.handleStatus(koFinalEvent.events[1])).toBe('Erreur');
    expect(LogbookComponent.handleStatus(runningEvent.events[1])).toBe('En cours');
  });
});
