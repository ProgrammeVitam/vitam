import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveObjectGroupComponent } from './archive-object-group.component';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";
import { KeysPipe, BytesPipe } from '../../../common/utils/pipes';
import { ReferentialsService } from "../../../referentials/referentials.service";
import { ResourcesService } from "../../../common/resources.service";
import { RouterTestingModule } from "@angular/router/testing";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

let ReferentialsServiceStub = {
  getAccessContract: (criteria) => Observable.of(new VitamResponse())
};

let ResourcesServiceStub = {
  getAccessContract: () => "ContractName"
};

describe('ArchiveObjectGroupComponent', () => {
  let component: ArchiveObjectGroupComponent;
  let fixture: ComponentFixture<ArchiveObjectGroupComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub },
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: ResourcesService, useValue: ResourcesServiceStub}
      ],
      declarations: [ ArchiveObjectGroupComponent, BytesPipe, KeysPipe ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveObjectGroupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should have access to object download', () => {
    component.userContract = {
      'DataObjectVersion': [
        "PhysicalMaster",
        "Dissemination"
      ]
    };
    expect(component.isDownloadable('PhysicalMaster_0')).toBeTruthy();

  });

  it('should not have access to object download', () => {
    component.userContract = {
      'DataObjectVersion': [
        "PhysicalMaster",
        "Dissemination"
      ]
    };
    expect(component.isDownloadable('BinaryMaster_0')).toBeFalsy();
  });
});
