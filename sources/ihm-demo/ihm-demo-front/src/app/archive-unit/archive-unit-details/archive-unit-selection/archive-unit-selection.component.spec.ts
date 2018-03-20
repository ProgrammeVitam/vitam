import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ArchiveUnitSelectionComponent } from './archive-unit-selection.component';

describe('ArchiveUnitSelectionComponent', () => {
  let component: ArchiveUnitSelectionComponent;
  let fixture: ComponentFixture<ArchiveUnitSelectionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ArchiveUnitSelectionComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
