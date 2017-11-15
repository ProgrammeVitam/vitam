import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { AccessionRegisterSearchComponent } from './accession-register.component';
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials/referentials.service";

const ReferentialsServiceStub = {
  getResults: (id) => Observable.of({"$hits":{"total":1,"offset":0,"limit":125,"size":10000}, '$results': [{}]}),
  setSearchAPI: () => {}
};

describe('AccessionRegisterSearchComponent', () => {
  let component: AccessionRegisterSearchComponent;
  let fixture: ComponentFixture<AccessionRegisterSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule.withRoutes([
        { path: 'ingest/sip', component: AccessionRegisterSearchComponent }
      ])
      ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub }
      ],
      declarations: [ AccessionRegisterSearchComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AccessionRegisterSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
