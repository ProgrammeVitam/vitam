import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ArchiveUnitComponent } from './archive-unit.component';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { VitamResponse } from "../../common/utils/response";
import { ArchiveUnitService } from "../archive-unit.service";
import { ArchiveUnitHelper } from "../archive-unit.helper";
import { RouterTestingModule } from "@angular/router/testing";
import { MySelectionService } from '../../my-selection/my-selection.service';
import { ResourcesService } from '../../common/resources.service';
import { DialogService } from '../../common/dialog/dialog.service';

let ArchiveUnitServiceStub = {
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

describe('ArchiveUnitComponent', () => {
  let component: ArchiveUnitComponent;
  let fixture: ComponentFixture<ArchiveUnitComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      declarations: [ ArchiveUnitComponent ],
      providers: [
        ArchiveUnitHelper,
        BreadcrumbService,
        DialogService,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub },
        { provide: MySelectionService, useValue: MySelectionServiceStub },
        { provide: ResourcesService, useValue: ResourceServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should filter options for the given rule category', () => {
    let dynamicActions = component.makeDynamicFinalActions();
    let filteredActions = ArchiveUnitComponent.computeFinalActions(dynamicActions, 'AppraisalRule');
    expect(filteredActions.length).toBe(2);
    filteredActions = ArchiveUnitComponent.computeFinalActions(dynamicActions, 'StorageRule');
    expect(filteredActions.length).toBe(3);
    filteredActions = ArchiveUnitComponent.computeFinalActions(dynamicActions, 'AccessRule');
    expect(filteredActions.length).toBe(0);
  });
});
