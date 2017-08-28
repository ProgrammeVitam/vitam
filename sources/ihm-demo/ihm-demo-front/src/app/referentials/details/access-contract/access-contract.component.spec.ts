import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { AccessContractComponent } from './access-contract.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { VitamResponse } from "../../../common/utils/response";

const ReferentialsServiceStub = {
  getAccessContractById: (id) => Observable.of({'$results': [{}]})
};

describe('AccessContractComponent', () => {
  let component: AccessContractComponent;
  let fixture: ComponentFixture<AccessContractComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub }
      ],
      declarations: [ AccessContractComponent ],
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
});
