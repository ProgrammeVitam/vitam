import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';

import { ArchiveUnitSelectionComponent } from './archive-unit-selection.component';
import {MySelectionService} from '../../../my-selection/my-selection.service';
import {ResourcesService} from '../../../common/resources.service';

describe('ArchiveUnitSelectionComponent', () => {
  let component: ArchiveUnitSelectionComponent;
  let fixture: ComponentFixture<ArchiveUnitSelectionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ArchiveUnitSelectionComponent ],
      providers: [
        { provide: MySelectionService, useValue: {} },
        { provide: ResourcesService, useValue: { getTenant: () => '1'}}
      ],
      schemas: [NO_ERRORS_SCHEMA]
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
