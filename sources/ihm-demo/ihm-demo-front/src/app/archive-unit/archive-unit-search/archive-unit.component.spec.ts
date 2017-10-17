import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ArchiveUnitComponent } from './archive-unit.component';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { VitamResponse } from "../../common/utils/response";
import { ArchiveUnitService } from "../archive-unit.service";
import { ArchiveUnitHelper } from "../archive-unit.helper";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveUnitComponent', () => {
  let component: ArchiveUnitComponent;
  let fixture: ComponentFixture<ArchiveUnitComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ArchiveUnitComponent ],
      providers: [
        ArchiveUnitHelper,
        BreadcrumbService,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
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
});
