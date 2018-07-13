import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MassiveUpdateFormComponent } from './massive-update-form.component';

describe('MassiveUpdateFormComponent', () => {
  let component: MassiveUpdateFormComponent;
  let fixture: ComponentFixture<MassiveUpdateFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MassiveUpdateFormComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MassiveUpdateFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
