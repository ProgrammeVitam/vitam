import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ArchiveUnitDetailsComponent } from './archive-unit-details.component';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { VitamResponse } from "../../common/utils/response";
import { ArchiveUnitService } from "../archive-unit.service";

let DefaultResponse = {
  $context: {},
  $hits: {},
  $results: [{'#object': '', '#operations': ['operationId']}],
  httpCode: 200
};

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(DefaultResponse),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveUnitDetailsComponent', () => {
  let component: ArchiveUnitDetailsComponent;
  let fixture: ComponentFixture<ArchiveUnitDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      declarations: [ ArchiveUnitDetailsComponent ],
      providers: [
        BreadcrumbService,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
