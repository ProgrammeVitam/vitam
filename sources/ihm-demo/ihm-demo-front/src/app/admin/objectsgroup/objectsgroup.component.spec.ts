import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs";

import { ObjectsGroupComponent } from './objectsgroup.component';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { VitamResponse } from "../../common/utils/response";
import { ObjectsGroupService } from "./objectsgroup.service";
import { ObjectsGroupHelper } from "./objectsgroup.helper";
import { RouterTestingModule } from "@angular/router/testing";
import { MySelectionService } from '../../my-selection/my-selection.service';
import { ResourcesService } from '../../common/resources.service';
import { DialogService } from '../../common/dialog/dialog.service';

let ObjectsGroupServiceStub = {
getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

let MySelectionServiceStub = {
  addToSelection: () => {},
  getIdsToSelect: () => Observable.of(new VitamResponse())
};

let ResourceServiceStub = {
  getTenant: () => "0"
};

describe('ObjectsGroupComponent', () => {
  let component: ObjectsGroupComponent;
  let fixture: ComponentFixture<ObjectsGroupComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      declarations: [ ObjectsGroupComponent ],
      providers: [
        ObjectsGroupHelper,
        BreadcrumbService,
        DialogService,
        { provide: ObjectsGroupService, useValue: ObjectsGroupServiceStub },
        { provide: MySelectionService, useValue: MySelectionServiceStub },
        { provide: ResourcesService, useValue: ResourceServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectsGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});