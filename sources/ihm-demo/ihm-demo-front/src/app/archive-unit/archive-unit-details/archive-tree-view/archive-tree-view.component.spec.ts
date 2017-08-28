import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveTreeViewComponent } from './archive-tree-view.component';
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";
import { KeysPipe } from '../../../common/utils/pipes';

let DefaultResponse = {
  $context: {},
  $hits: {},
  $results: [],
  httpCode: 200
};

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(DefaultResponse),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveTreeViewComponent', () => {
  let component: ArchiveTreeViewComponent;
  let fixture: ComponentFixture<ArchiveTreeViewComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      declarations: [ ArchiveTreeViewComponent ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveTreeViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
