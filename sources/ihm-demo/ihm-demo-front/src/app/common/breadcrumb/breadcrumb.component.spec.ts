import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BreadcrumbComponent } from './breadcrumb.component';
import {BreadcrumbModule, DialogModule, FieldsetModule, PanelModule} from 'primeng/primeng';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BreadcrumbElement, BreadcrumbService} from '../breadcrumb.service';
import {Observable} from 'rxjs/Observable';

let value: BreadcrumbElement[];
const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};

describe('BreadcrumbComponent', () => {
  let component: BreadcrumbComponent;
  let fixture: ComponentFixture<BreadcrumbComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BreadcrumbComponent ],
      imports: [BreadcrumbModule, PanelModule, BrowserAnimationsModule],
      providers: [
        {provide: BreadcrumbService, useValue: BreadcrumbServiceStub}
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BreadcrumbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
