import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ArchiveUnitProfileComponent } from './archive-unit-profile.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DialogService } from "../../../common/dialog/dialog.service";
import { ErrorService } from "../../../common/error.service";

const ReferentialsServiceStub = {
  getArchiveUnitProfileById: (id) => Observable.of({'$results': [{}]})
};

describe('ArchiveUnitProfileComponent', () => {
  let component: ArchiveUnitProfileComponent;
  let fixture: ComponentFixture<ArchiveUnitProfileComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        ErrorService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: DialogService, useValue: {} }
      ],
      declarations: [ ArchiveUnitProfileComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should change status', () => {
    component.updatedFields = {};
    
    // Click on status selector update isActif and trigger changeStatus
    component.isActif = true;
    component.changeStatus();
    expect(component.updatedFields.Status).toBe('ACTIVE');

    // Click on status selector update isActif and trigger changeStatus
    component.isActif = false;
    component.changeStatus();
    expect(component.updatedFields.Status).toBe('INACTIVE');
  });
});
