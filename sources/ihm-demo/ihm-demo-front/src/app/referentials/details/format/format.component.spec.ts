import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { FormatComponent } from './format.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import {ErrorService} from "../../../common/error.service";
import {DialogService} from "../../../common/dialog/dialog.service";

const ReferentialsServiceStub = {
  getFormatById: (id) => Observable.of({'$results': [{}]})
};

describe('FormatComponent', () => {
  let component: FormatComponent;
  let fixture: ComponentFixture<FormatComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        ErrorService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: DialogService, useValue: {} }
      ],
      declarations: [ FormatComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FormatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
