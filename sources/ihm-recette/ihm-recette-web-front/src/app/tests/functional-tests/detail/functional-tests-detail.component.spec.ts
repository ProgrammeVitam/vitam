import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {PanelModule, DataTableModule, FieldsetModule} from 'primeng/primeng';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import {Observable} from "rxjs/Observable";
import 'rxjs/add/observable/of';

import { FunctionalTestsDetailComponent } from './functional-tests-detail.component';
import { FunctionalTestsService } from '../functional-tests.service';
import { BreadcrumbElement, BreadcrumbService } from '../../../common/breadcrumb.service';
import { GenericTableComponent } from '../../../common/generic-table/generic-table.component';
import {RouterTestingModule} from '@angular/router/testing';

let mockResults = [{
  "Feature" : "initialisation",
  "Description" : "Sc\u00E9nario 0 import des regles des getions et formats",
  "Errors" : [ ],
  "Ok" : true
}];
let BreadcrumbServiceStub = {
  changeState: (api) => Observable.of('OK'),
};

let ActivatedRouteStub = {
  paramMap: {
    get : (name) => Observable.of("fileName1"),
    switchMap: () => Observable.of({
      Reports : mockResults,
      NumberOfTestOK : 1,
      NumberOfTestKO : 0
    })
  },
};

let FunctionalTestsServiceStub = {
  getResultDetail: (api) => Observable.of({})
};


describe('FunctionalTestsDetailComponent', () => {
  let component: FunctionalTestsDetailComponent;
  let fixture: ComponentFixture<FunctionalTestsDetailComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FunctionalTestsDetailComponent, GenericTableComponent ],
      imports: [PanelModule, DataTableModule, BrowserAnimationsModule, FieldsetModule, RouterTestingModule],
      providers: [
        { provide: FunctionalTestsService, useValue: FunctionalTestsServiceStub },
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub},
        { provide: ActivatedRoute, useValue: ActivatedRouteStub}
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FunctionalTestsDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created with all details', () => {
    expect(component).toBeTruthy();
    expect(component.resultDetail.Reports).toEqual(mockResults);
    expect(component.resultDetail.NumberOfTestOK).toEqual(1);
    expect(component.resultDetail.NumberOfTestKO).toEqual(0);
    expect(component.cols).toEqual([
      {field: 'Feature', label: 'Fonctionnalité'},
      {field: 'OperationId', label: `Identifiant de l'opération`},
      {field: 'Description', label: 'Description'},
      {field: 'Errors', label: 'Erreurs'}
    ]);
  });
});
