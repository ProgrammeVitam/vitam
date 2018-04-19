import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { OntologyComponent } from './ontology.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DialogService } from "../../../common/dialog/dialog.service";
import { ErrorService } from "../../../common/error.service";

const ReferentialsServiceStub = {
  getOntologyById: (id) => Observable.of({'$results': [{}]})
};

describe('OntologyComponent', () => {
  let component: OntologyComponent;
  let fixture: ComponentFixture<OntologyComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        ErrorService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: DialogService, useValue: {} }
      ],
      declarations: [OntologyComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OntologyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });


});
