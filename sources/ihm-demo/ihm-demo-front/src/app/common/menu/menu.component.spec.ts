import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MenuComponent } from './menu.component';
import {ButtonModule, GrowlModule, MenubarModule} from 'primeng/primeng';
import {RouterTestingModule} from '@angular/router/testing';
import {FormsModule} from '@angular/forms';
import {ResourcesService} from '../resources.service';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  delete: (url) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};

describe('MenuComponent', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MenuComponent ],
      imports: [ MenubarModule, ButtonModule, RouterTestingModule, GrowlModule, FormsModule ],
      providers: [
        { provide: ResourcesService, useValue: ResourcesServiceStub }
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
