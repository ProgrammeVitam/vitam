import { PanelModule, DataTableModule, ButtonModule} from 'primeng/primeng';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { FunctionalTestsComponent } from './functional-tests.component';
import { GenericTableComponent } from '../../common/generic-table/generic-table.component';
import { FunctionalTestsService } from './functional-tests.service';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import {Observable} from "rxjs/Observable";
import 'rxjs/add/observable/of';
import {RouterTestingModule} from '@angular/router/testing';


let BreadcrumbServiceStub = {
  changeState: (api) => Observable.of('OK'),
};

let FunctionalTestsServiceStub = {
  getResults: (api) => Observable.of(['fileNam1', 'fileName2']),
  launchTests: () => Observable.of('OK'),
};

describe('FunctionalTestsComponent', () => {
  let component: FunctionalTestsComponent;
  let fixture: ComponentFixture<FunctionalTestsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FunctionalTestsComponent, GenericTableComponent ],
      imports: [PanelModule, DataTableModule, BrowserAnimationsModule, ButtonModule, RouterTestingModule],
      providers: [
        { provide: FunctionalTestsService, useValue: FunctionalTestsServiceStub },
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub}
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FunctionalTestsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created with correct results', () => {
    expect(component).toBeTruthy();
    expect(component.cols).toEqual([
      {field: 'value', label: 'Rapport'},
      {field: 'details', label: 'Détails'}
    ]);
    expect(component.results).toEqual([
      { value: 'fileNam1', details: 'Accès au détail' },
      { value: 'fileName2', details: 'Accès au détail' }
    ]);
  });

  it('should be created with result', () => {
    component.launchTests();
    expect(component.pending).toEqual(false);
    expect(component.error).toEqual(true);
  });
});
