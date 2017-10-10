import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {Observable} from 'rxjs/Observable';
import {DropdownModule, InputTextModule, SelectItem, DataTableModule} from 'primeng/primeng';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {FormsModule} from '@angular/forms';
import 'rxjs/add/observable/of';

import { GenericTableComponent } from '../../common/generic-table/generic-table.component';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import { PerfComponent } from './perf.component';
import { PerfService } from './perf.service';
import {RouterTestingModule} from '@angular/router/testing';

let value: BreadcrumbElement[];
const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};

const PerfServiceStub = {
  generateIngestStatReport: () => Observable.of(["report1","report2","report3"]),
  getAvailableSipForUpload: () => Observable.of(["SIP1","SIP2","SIP3"]),
};

describe('PerfComponent', () => {
  let component: PerfComponent;
  let fixture: ComponentFixture<PerfComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PerfComponent, GenericTableComponent ],
      providers: [
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub },
        { provide: PerfService, useValue: PerfServiceStub }
      ],
      imports: [
        InputTextModule,
        FormsModule,
        DropdownModule,
        BrowserAnimationsModule,
        DataTableModule,
        RouterTestingModule
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PerfComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created with value', () => {
    expect(component).toBeTruthy();
    expect(component.reportsList).toEqual([ {value : "report1"},{value : "report2"},{value : "report3"}]);
    expect(component.sipList).toEqual([ {value : "SIP1", label : "SIP1"},{value : "SIP2", label : "SIP2"},{value : "SIP3", label : "SIP3"}]);
  });
});
