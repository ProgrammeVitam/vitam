import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {CollectionComponent} from './collection.component';
import {RemoveItemsComponent} from './remove-items/remove-items.component';
import {DialogModule, FieldsetModule, PanelModule} from 'primeng/primeng';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import {CollectionService} from './collection.service';
import {ResourcesService} from '../../common/resources.service';

const CollectionServiceStub = {
  removeItemsInCollection: (api) => Observable.of('OK'),
};

let value: BreadcrumbElement[];
const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  delete: (url) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};

describe('CollectionComponent', () => {
  let component: CollectionComponent;
  let fixture: ComponentFixture<CollectionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [CollectionComponent, RemoveItemsComponent],
      imports: [PanelModule, BrowserAnimationsModule, FieldsetModule, DialogModule],
      providers: [
        { provide: CollectionService, useValue: CollectionServiceStub },
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub },
        { provide: ResourcesService, useValue: ResourcesServiceStub}
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
