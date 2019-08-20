import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { AccessContractComponent } from './access-contract.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DialogService } from "../../../common/dialog/dialog.service";
import {AccessContract} from "./access-contract";
import {ReferentialHelper} from "../../referential.helper";
import {ErrorService} from "../../../common/error.service";

const ReferentialsServiceStub = {
  getAccessContractById: (id) => Observable.of({'$results': [{}]})
};

describe('AccessContractComponent', () => {
  let component: AccessContractComponent;
  let fixture: ComponentFixture<AccessContractComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        BreadcrumbService,
        ErrorService,
        {provide: ReferentialsService, useValue: ReferentialsServiceStub},
        {provide: DialogService, useValue: {}}
      ],
      declarations: [AccessContractComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AccessContractComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should switch to initial state', () => {
    component.contract = new AccessContract();
    component.contract.Name = 'InitialName';
    component.contract.Description = 'Initial Description';
    component.contract.DataObjectVersion = ['BinaryMaster'];
    component.contract.Status = 'Inactif';
    component.isActif = false;
    component.contract.AccessLog = 'Inactif';
    component.accessLogIsActif = false;

    component.modifiedContract = new AccessContract();
    component.modifiedContract.Name = 'UpdatedName';
    component.modifiedContract.Description = 'Initial Description';
    component.modifiedContract.DataObjectVersion = ['BinaryMaster', 'Thumbnail'];
    component.modifiedContract.Status = 'Actif';
    component.isActif = true;
    component.modifiedContract.AccessLog = 'Actif';
    component.accessLogIsActif = true;
    component.update = true;

    component.switchUpdateMode();

    expect(component.update).toBeFalsy();
    expect(component.modifiedContract.Name).toBe('InitialName');
    expect(component.modifiedContract.Description).toBe('Initial Description');
    expect(component.modifiedContract.DataObjectVersion.length).toBe(1);
    expect(component.modifiedContract.Status).toBe('Inactif');
    expect(component.isActif).toBeFalsy();
    expect(component.modifiedContract.AccessLog).toBe('Inactif');
    expect(component.accessLogIsActif).toBeFalsy();
  });

  it('should switch to update mode', () => {
    component.updatedFields = {'DataObjectVersion': ['BinaryMaster', 'Thumbnail']};
    component.update = false;

    component.switchUpdateMode();

    expect(component.update).toBeTruthy();
    expect(component.updatedFields.DataObjectVersion).toBeUndefined();
  });

  it('should update DataObjectVersion on EveryDataObjectVersion change', () => {
    component.contract.DataObjectVersion = ['BinaryMaster'];
    component.updatedFields.DataObjectVersion = ['BinaryMaster'];
    component.modifiedContract.DataObjectVersion = ['BinaryMaster'];
    component.updatedFields.EveryDataObjectVersion = false;

    component.modifiedContract.EveryDataObjectVersion = true;
    component.changeBooleanValue('EveryDataObjectVersion');

    expect(component.updatedFields.EveryDataObjectVersion).toBeTruthy();
    expect(component.modifiedContract.DataObjectVersion.length).toBe(ReferentialHelper.optionLists.DataObjectVersion.length);
    expect(component.updatedFields.DataObjectVersion.length).toBe(ReferentialHelper.optionLists.DataObjectVersion.length);

    component.modifiedContract.EveryDataObjectVersion = false;
    component.changeBooleanValue('EveryDataObjectVersion');

    expect(component.updatedFields.EveryDataObjectVersion).toBeFalsy();
    expect(component.modifiedContract.DataObjectVersion.length).toBe(component.contract.DataObjectVersion.length);
    expect(component.updatedFields.DataObjectVersion).toBeUndefined();
  });

  it('should change status', () => {
    component.updatedFields = {};

    // Click on status selector update isActif and trigg changeStatus
    component.isActif = true;
    component.changeStatus();

    expect(component.updatedFields.Status).toBe('ACTIVE');

    // Click on status selector update isActif and trigg changeStatus
    component.isActif = false;
    component.changeStatus();

    expect(component.updatedFields.Status).toBe('INACTIVE');
  });

  it('should change access log', () => {
    component.updatedFields = {};

    // Click on status selector update accessLogIsActif and trigg changeStatus
    component.accessLogIsActif = true;
    component.changeAccessLog();

    expect(component.updatedFields.AccessLog).toBe('ACTIVE');

    // Click on status selector update accessLogIsActif and trigg changeStatus
    component.accessLogIsActif = false;
    component.changeAccessLog();

    expect(component.updatedFields.AccessLog).toBe('INACTIVE');
  });

});
