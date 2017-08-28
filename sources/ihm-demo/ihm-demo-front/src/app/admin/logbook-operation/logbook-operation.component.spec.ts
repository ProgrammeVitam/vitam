import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogbookOperationComponent } from './logbook-operation.component';

describe('LogbookOperationComponent', () => {
  let component: LogbookOperationComponent;
  let fixture: ComponentFixture<LogbookOperationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationComponent ]
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
