import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { AgenciesComponent } from './agencies.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { ActivatedRoute } from "@angular/router";

const ReferentialsServiceStub = {
  getAgenciesById: (id) => Observable.of({'$results': [{}]}),
  getFundRegisterById : (id) => Observable.of({"$hits":{"total":1,"offset":0,"limit":125,"size":10000}})
};

describe('AgenciesComponent', () => {
  let component: AgenciesComponent;
  let fixture: ComponentFixture<AgenciesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 'Mock_Service'})} }
      ],
      declarations: [ AgenciesComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AgenciesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should adapt breadcrumb regarding provided url', () => {
    component.updateBreadcrumb('accessionRegister');
    expect(component.newBreadcrumb.length).toBe(3);
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 1].label)
        .toBe('Détail du service producteur Mock_Service');

    component.updateBreadcrumb('all');
    expect(component.newBreadcrumb.length).toBe(3);
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 1].label)
        .toBe('Détail du service agent Mock_Service');
  });
});
