import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ManagementContractComponent } from './management-contract.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DialogService } from "../../../common/dialog/dialog.service";
import {ManagementContract, ManagementContractStorage} from "./management-contract";
import {ErrorService} from "../../../common/error.service";

const ReferentialsServiceStub = {
  getManagementContractById: () => Observable.of({'$results': [{Storage:{UnitStrategy:'unit-strategy'}}]})
};

describe('ManagementContractComponent', () => {
  let component: ManagementContractComponent;
  let fixture: ComponentFixture<ManagementContractComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        BreadcrumbService,
        ErrorService,
        {provide: ReferentialsService, useValue: ReferentialsServiceStub},
        {provide: DialogService, useValue: {}}
      ],
      declarations: [ManagementContractComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ManagementContractComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should switch to initial state', () => {
    component.contract = new ManagementContract();
    component.contract.Name = 'InitialName';
    component.contract.Description = 'Initial Description';
    component.contract.Status = 'Inactif';
    component.isActif = false;
    const storage = new ManagementContractStorage();
    storage.UnitStrategy = 'strategy-unit';
    storage.ObjectGroupStrategy = 'strategy-got';
    component.contract.Storage = storage;
    component.modifiedContract = new ManagementContract();
    component.modifiedContract.Name = 'UpdatedName';
    component.modifiedContract.Description = 'Initial Description';
    component.modifiedContract.Status = 'Actif';
    const modifiedStorage = new ManagementContractStorage();
    modifiedStorage.ObjectStrategy = 'strategy-object';
    component.modifiedContract.Storage = modifiedStorage;
    component.isActif = true;
    component.update = true;
    component.switchUpdateMode();
    expect(component.update).toBeFalsy();
    expect(component.modifiedContract.Name).toBe('InitialName');
    expect(component.modifiedContract.Description).toBe('Initial Description');
    expect(component.modifiedContract.Status).toBe('Inactif');
    expect(component.modifiedContract.Storage).toBeDefined();
    expect(component.modifiedContract.Storage.ObjectGroupStrategy).toBe('strategy-got');
    expect(component.modifiedContract.Storage.ObjectStrategy).toBeUndefined();
    expect(component.isActif).toBeFalsy();
  });

  it('should switch to update mode', () => {
    component.updatedFields = { 'Storage' : { 'ObjectStrategy':'strategy-object' } };
    component.update = false;

    component.switchUpdateMode();

    expect(component.update).toBeTruthy();
    expect(component.updatedFields.Storage).toBeUndefined();
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

});
