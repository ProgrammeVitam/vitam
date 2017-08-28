import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import {ConfirmationService} from "primeng/primeng";

import { ArchiveRuleBlocComponent } from './archive-rule-bloc.component';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";
import { KeysPipe } from '../../../common/utils/pipes';

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveRuleBlocComponent', () => {
  let component: ArchiveRuleBlocComponent;
  let fixture: ComponentFixture<ArchiveRuleBlocComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitHelper,
        ConfirmationService,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      declarations: [ ArchiveRuleBlocComponent, KeysPipe ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveRuleBlocComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
