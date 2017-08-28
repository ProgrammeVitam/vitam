import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveMainDescriptionComponent } from './archive-main-description.component';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveMainDescriptionComponent', () => {
  let component: ArchiveMainDescriptionComponent;
  let fixture: ComponentFixture<ArchiveMainDescriptionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      declarations: [ ArchiveMainDescriptionComponent ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveMainDescriptionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
