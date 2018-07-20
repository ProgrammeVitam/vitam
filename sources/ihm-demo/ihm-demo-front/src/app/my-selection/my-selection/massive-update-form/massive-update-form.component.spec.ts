import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MassiveUpdateFormComponent } from './massive-update-form.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { KeysPipe } from '../../../common/utils/pipes';
import {ArchiveUnitHelper} from '../../../archive-unit/archive-unit.helper';
import {DialogService} from '../../../common/dialog/dialog.service';

describe('MassiveUpdateFormComponent', () => {
  let component: MassiveUpdateFormComponent;
  let fixture: ComponentFixture<MassiveUpdateFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MassiveUpdateFormComponent, KeysPipe ],
      providers: [
        ArchiveUnitHelper,
        DialogService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
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
