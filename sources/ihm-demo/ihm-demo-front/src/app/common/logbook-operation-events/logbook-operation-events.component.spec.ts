import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { LogbookOperationEventsComponent } from './logbook-operation-events.component';
import { VitamResponse } from "../utils/response";

describe('LogbookOperationEventsComponent', () => {
  let component: LogbookOperationEventsComponent;
  let fixture: ComponentFixture<LogbookOperationEventsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationEventsComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookOperationEventsComponent);
    component = fixture.componentInstance;
    component.operation = {$results: [{events: []}]};
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
