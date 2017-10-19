import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Observable } from "rxjs/Rx";

import { CalendarModule, OverlayPanelModule, PanelModule } from 'primeng/primeng';

import { TreeSearchComponent } from './tree-search.component';

import { ArchiveUnitService } from "../../../archive-unit.service";
import { ArchiveUnitHelper } from "../../../archive-unit.helper";
import { VitamResponse } from "../../../../common/utils/response";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('TreeSearchComponent', () => {
  let component: TreeSearchComponent;
  let fixture: ComponentFixture<TreeSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      imports: [ BrowserAnimationsModule, CalendarModule, OverlayPanelModule, PanelModule, RouterTestingModule, ReactiveFormsModule ],
      declarations: [ TreeSearchComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TreeSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
