import { Observable } from 'rxjs/Observable';
import { PanelModule, FieldsetModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfigurationComponent } from './configuration.component';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ResourcesService } from '../../common/resources.service';

let value: BreadcrumbElement[];

const BreadcrumbServiceStub = {
  changeState: (myChange: BreadcrumbElement[]) => {
    value = myChange;
  },
  getState: () => Observable.of(value)
};

const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of([{ 'id': 'default', 'offers': [{ 'id': 'offer1' }] }])
};


describe('ConfigurationComponent', () => {
  let component: ConfigurationComponent;
  let fixture: ComponentFixture<ConfigurationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ConfigurationComponent],
      imports: [PanelModule, FieldsetModule, BrowserAnimationsModule],
      providers: [
        { provide: BreadcrumbService, useValue: BreadcrumbServiceStub },
        { provide: ResourcesService, useValue: ResourcesServiceStub }
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
