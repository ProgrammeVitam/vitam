import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Observable } from "rxjs/Rx";
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

import { CalendarModule, OverlayPanelModule, PanelModule } from 'primeng/primeng';

import { TreeChildComponent } from './tree-child.component';
import { ArchiveUnitService } from "../../../archive-unit.service";
import { VitamResponse } from "../../../../common/utils/response";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('TreeChildComponent', () => {
  let component: TreeChildComponent;
  let fixture: ComponentFixture<TreeChildComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      imports: [ BrowserAnimationsModule, CalendarModule, RouterTestingModule, OverlayPanelModule, PanelModule, FormsModule, ReactiveFormsModule ],
      declarations: [ TreeChildComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TreeChildComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
