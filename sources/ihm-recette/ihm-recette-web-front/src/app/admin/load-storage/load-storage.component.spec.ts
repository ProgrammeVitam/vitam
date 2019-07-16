import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LoadStorageComponent } from './load-storage.component';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {TenantService} from '../../common/tenant.service';
import {ResourcesService} from '../../common/resources.service';
import {LoadStorageService} from './load-storage.service';
import {Observable} from 'rxjs/Observable';
import {Title} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';

const LoadStorageServiceStub = {
  download: (fileName, category) => {
    return Observable.of('data');
  },
  uploadFile: (newFile, fileName, category) => {
    return Observable.of({});
  }
};

let value: BreadcrumbElement[];
const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};

const cookies = {'tenant': 0};
const ResourcesServiceStub = {
  getTenants: () => Observable.of(['0', '1', '2']),
  getTenant: () => cookies['tenant'],
  get: (url, header?: Headers) => Observable.of([{'id':'default', 'offers':[{'id':'offer1'}]}])

};

const TenantServiceStub = {
  changeState:  (myChange: string) => Observable.of('OK'),
  getState: () => Observable.of('OK')
};

describe('LoadStorageComponent', () => {
  let component: LoadStorageComponent;
  let fixture: ComponentFixture<LoadStorageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LoadStorageComponent ],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        Title,
        { provide: LoadStorageService, useValue: LoadStorageServiceStub },
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub },
        { provide: ResourcesService, useValue: ResourcesServiceStub },
        { provide: TenantService, useValue: TenantServiceStub }
      ],
      imports: [
        RouterTestingModule.withRoutes([
          {
            path: 'load-storage', component: LoadStorageComponent
          }, {
            path: 'admin/collection', component: LoadStorageComponent
          }
        ])
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoadStorageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
