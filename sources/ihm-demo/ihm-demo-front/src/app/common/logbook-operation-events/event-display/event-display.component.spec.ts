import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from "@angular/core";

import {EventDisplayComponent} from './event-display.component';
import {Event} from '../event';
import {TranslateModule} from "@ngx-translate/core";
import {RouterTestingModule} from "@angular/router/testing";

describe('EventDisplayComponent', () => {
  let component: EventDisplayComponent;
  let fixture: ComponentFixture<EventDisplayComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        TranslateModule.forRoot({})
    ],
      declarations: [EventDisplayComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EventDisplayComponent);
    component = fixture.componentInstance;
    component.event = new Event({}, {'outcome': 'OK', 'evType': 'STP_OBJ_STORING'}, []);
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
