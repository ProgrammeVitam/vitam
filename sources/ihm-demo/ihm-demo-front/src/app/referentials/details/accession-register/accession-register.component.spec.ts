import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { LogbookService } from "../../../ingest/logbook.service";
import { AccessionRegisterComponent } from './accession-register.component';
import { ActivatedRoute } from "@angular/router";

const ReferentialsServiceStub = {
  getFundRegisterDetailById: (id) => Observable.of({'$results': [{}]}),
  getFundRegisterById : (id) => Observable.of({"$hits":{"total":1,"offset":0,"limit":125,"size":10000}, '$results': [{}]})
};

const LogbookServiceStub = {
  getResults: (id) => Observable.of({'$results': [{}]}),
};

describe('AccessionRegisterComponent', () => {
  let component: AccessionRegisterComponent;
  let fixture: ComponentFixture<AccessionRegisterComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: LogbookService, useValue: LogbookServiceStub },
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 'Mock_Service'})} }
      ],
      declarations: [ AccessionRegisterComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AccessionRegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should adapt breadcrumb regarding provided url', () => {
    component.updateBreadcrumb('all');
    expect(component.newBreadcrumb.length).toBe(4);
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 2].label)
        .toBe('Détail du service agent Mock_Service');
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 1].label)
        .toBe('Détail du fonds Mock_Service');

    component.updateBreadcrumb('accessionRegister');
    expect(component.newBreadcrumb.length).toBe(4);
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 2].label)
        .toBe('Détail du service producteur Mock_Service');
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 1].label)
        .toBe('Détail du fonds Mock_Service');
  });
});
