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

const binaryMaster = {
  DataObjectVersion:"BinaryMaster_0",
  metadatas: {}
}

const physicalMaster = {
  DataObjectVersion:"PhysicalMaster_0",
  metadatas: {
    PhysicalId:'10'
  }
}

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

  it('should have access to object download if version match', () => {
    component.userContract = {
      'EveryDataObjectVersion': false,
      'DataObjectVersion': [
        "PhysicalMaster",
        "BinaryMaster"
      ]
    };
    expect(component.isDownloadable(binaryMaster)).toBeTruthy();
  });

  it('should not have access to object download for physical object', () => {
    component.userContract = {
      'EveryDataObjectVersion': false,
      'DataObjectVersion': [
        "PhysicalMaster",
        "BinaryMaster"
      ]
    };
    expect(component.isDownloadable(physicalMaster)).toBeFalsy();
  });

  it('should not have access to object if version don\'t match', () => {
    component.userContract = {
      'EveryDataObjectVersion': false,
      'DataObjectVersion': [
        "PhysicalMaster",
        "Dissemination"
      ]
    };
    expect(component.isDownloadable(binaryMaster)).toBeFalsy();
  });

  it('should have access to object if everyVersion accepted', () => {
    component.userContract = {
      'EveryDataObjectVersion': true
    };
    expect(component.isDownloadable(binaryMaster)).toBeTruthy();
  });

  it ('should not throw error if versions empty', () => {
    component.userContract = {
      'EveryDataObjectVersion': false
    };
    expect(component.isDownloadable(binaryMaster)).toBeFalsy();
  });

});
