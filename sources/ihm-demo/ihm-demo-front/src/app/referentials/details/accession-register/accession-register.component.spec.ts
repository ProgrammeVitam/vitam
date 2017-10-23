import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { VitamResponse } from "../../../common/utils/response";
import { LogbookService } from "../../../ingest/logbook.service";
import { AccessionRegisterComponent } from './accession-register.component';

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
        { provide: LogbookService, useValue: LogbookServiceStub }
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
});
