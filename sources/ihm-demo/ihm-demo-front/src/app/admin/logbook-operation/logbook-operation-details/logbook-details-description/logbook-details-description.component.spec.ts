import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogbookDetailsDescriptionComponent } from './logbook-details-description.component';
import { LogbookService } from "../../../../ingest/logbook.service";
import { Observable } from "rxjs";
import { VitamResponse } from "../../../../common/utils/response";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import {Logbook} from "../../../../common/utils/logbook";

const LogbookServiceStub = {
  getDetails: () => Observable.of({'$results': [new Logbook()]})
};

describe('LogbookDetailsDescriptionComponent', () => {
  let component: LogbookDetailsDescriptionComponent;
  let fixture: ComponentFixture<LogbookDetailsDescriptionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookDetailsDescriptionComponent ],
      providers: [
        { provide: LogbookService, useValue: LogbookServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookDetailsDescriptionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
