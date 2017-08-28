import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ContextComponent } from './context.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { VitamResponse } from "../../../common/utils/response";

const ReferentialsServiceStub = {
  getContextById: (id) => Observable.of({'$results': [{}]})
};

describe('ContextComponent', () => {
  let component: ContextComponent;
  let fixture: ComponentFixture<ContextComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub }
      ],
      declarations: [ ContextComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ContextComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
