import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FieldsetModule } from 'primeng/primeng';

import { LogbookOperationDetailsComponent } from './logbook-operation-details.component';
import { LogbookOperationEventsComponent } from '../../../common/logbook-operation-events/logbook-operation-events.component';
import { EventDisplayComponent } from '../../../common/logbook-operation-events/event-display/event-display.component';

describe('LogbookOperationDetailsComponent', () => {
  let component: LogbookOperationDetailsComponent;
  let fixture: ComponentFixture<LogbookOperationDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationDetailsComponent, LogbookOperationEventsComponent, EventDisplayComponent ],
      imports: [ BrowserAnimationsModule, FieldsetModule ]
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
