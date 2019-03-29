import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { IngestContractComponent } from './ingest-contract.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DialogService } from "../../../common/dialog/dialog.service";
import {IngestContract} from "./ingest-contract";
import {ErrorService} from "../../../common/error.service";

const ReferentialsServiceStub = {
  getIngestContractById: (id) => Observable.of({'$results': [{CheckParentLink: "AUTHORIZED"}]})
};

describe('IngestContractComponent', () => {
  let component: IngestContractComponent;
  let fixture: ComponentFixture<IngestContractComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        ErrorService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: DialogService, useValue: {} }
      ],
      declarations: [ IngestContractComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IngestContractComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should switch to initial state', () => {
    component.contract = new IngestContract();
    component.contract.Name = 'InitialName';
    component.contract.Description = 'Initial Description';
    component.contract.Status = 'Inactif';
    component.contract.CheckParentLink = 'AUTHORIZED';
    component.isActif = false;

    component.modifiedContract = new IngestContract();
    component.modifiedContract.Name = 'UpdatedName';
    component.modifiedContract.Description = 'Initial Description';
    component.modifiedContract.Status = 'Actif';
    component.modifiedContract.CheckParentLink = 'UNAUTHORIZED';
    component.isActif = true;
    component.update = true;

    component.switchUpdateMode();

    expect(component.update).toBeFalsy();
    expect(component.modifiedContract.Name).toBe('InitialName');
    expect(component.modifiedContract.Description).toBe('Initial Description');
    expect(component.modifiedContract.Status).toBe('Inactif');
    expect(component.isActif).toBeFalsy();
  });

  it('should change status', () => {
    component.updatedFields = {};
    component.isActif = false;

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
