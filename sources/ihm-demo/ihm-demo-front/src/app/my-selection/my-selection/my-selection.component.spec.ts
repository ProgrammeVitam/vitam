import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';

import {MySelectionComponent} from './my-selection.component';
import {BreadcrumbService} from '../../common/breadcrumb.service';
import {ArchiveUnitHelper} from '../../archive-unit/archive-unit.helper';
import {Observable} from 'rxjs/Rx';
import {RouterTestingModule} from '@angular/router/testing';
import {MySelectionService} from '../my-selection.service';
import {ArchiveUnitService} from '../../archive-unit/archive-unit.service';
import {DialogService} from '../../common/dialog/dialog.service';
import {VitamResponse} from '../../common/utils/response';
import {ArchiveUnitMetadata, ArchiveUnitSelection} from '../selection';
import {ObjectsService} from '../../common/utils/objects.service';
import {ReferentialsService} from '../../referentials/referentials.service';
import {ResourcesService} from "../../common/resources.service";

const response: VitamResponse = {
  httpCode: 200,
  $results: [{}],
  $hits: {
    total: 1,
    size: 1,
    limit: 125,
    offset: 0
  },
  $context: {
    $query: {},
    $projection: {},
    $filter: {}
  },
  $facetResults: []
};

const defaultMetadata: ArchiveUnitMetadata = {
  '#id': 'id',
  Title: 'Titre',
  EndDate: '',
  StartDate: '',
  '#unitType': '',
  '#object': ''
};

const defaultAUSMock: ArchiveUnitSelection[] = [
  {
    archiveUnitMetadata: defaultMetadata,
    haveChildren: true,
    selected: false,
    isChild: false,
    children: [],
    displayChildren: false
  },
  {
    archiveUnitMetadata: defaultMetadata,
    haveChildren: false,
    selected: false,
    isChild: false,
    children: [],
    displayChildren: false
  }
];

const MySelectionServiceStub = {
  getResults: (offset, limit: number = 125) => Observable.of(response),
  getBasketFromLocalStorage: () => [{}],
  haveChildren: () => true,
  getChildren: (id) => {
    response.$results.push({
      '#id': id,
      Title: 'Titre',
      EndDate: '',
      StartDate: '',
      '#unitType': '',
      '#object': ''
    });
    return Observable.of(response);
  }
};

const ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(response),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

const ResourceServiceStub = {
  getTenant: () => 0
};

const ReferentialsServiceStub = {
  getResults: (id) => Observable.of({"$hits":{"total":1,"offset":0,"limit":125,"size":10000}, '$results': [{}]}),
  setSearchAPI: () => {}
};

describe('MySelectionComponent', () => {
  let component: MySelectionComponent;
  let fixture: ComponentFixture<MySelectionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([
          {
            path: 'basket',
            component: MySelectionComponent
          }, {
            path: 'ingest/sip', component: MySelectionComponent, data : {permission : 'ingest:create'}
          }
        ])
      ],
      declarations: [MySelectionComponent],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        DialogService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: MySelectionService, useValue: MySelectionServiceStub },
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub },
        { provide: ResourcesService, useValue: ResourceServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MySelectionComponent);
    component = fixture.componentInstance;
    component.getColumns = () => [];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should check if all items are selected or not', () => {
    component.selectedArchiveUnits = ObjectsService.clone(defaultAUSMock);
    expect(component.isAllChecked()).toBeFalsy();
    component.selectedArchiveUnits[0].selected = true;
    expect(component.isAllChecked()).toBeFalsy();
    component.selectedArchiveUnits[1].selected = true;
    expect(component.isAllChecked()).toBeTruthy();
  });

  it('should count the good number of selected items', () => {
    component.selectedArchiveUnits = ObjectsService.clone(defaultAUSMock);
    expect(component.countSelected()).toBe(0);
    component.selectedArchiveUnits[0].selected = true;
    expect(component.countSelected()).toBe(1);
    component.selectedArchiveUnits[1].selected = true;
    expect(component.countSelected()).toBe(2);
  });

  it('should select or deselect all items', () => {
    component.selectedArchiveUnits = ObjectsService.clone(defaultAUSMock);
    expect(component.countSelected()).toBe(0);
    component.checkAll();
    expect(component.countSelected()).toBe(2);
    component.checkAll();
    expect(component.countSelected()).toBe(0);
    component.selectedArchiveUnits[1].selected = true;
    expect(component.countSelected()).toBe(1);
    component.checkAll();
    expect(component.countSelected()).toBe(2);
  });

  it('should inverse the selection of an item', () => {
    const item = ObjectsService.clone(defaultAUSMock)[0];
    expect(item.selected).toBeFalsy();

    component.inverseSelection(item);
    expect(item.selected).toBeTruthy();

    component.inverseSelection(item);
    expect(item.selected).toBeFalsy();
  });
});
