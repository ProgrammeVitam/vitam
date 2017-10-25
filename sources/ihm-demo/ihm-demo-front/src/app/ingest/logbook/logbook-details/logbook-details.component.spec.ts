import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { LogbookDetailsComponent } from './logbook-details.component';
import {Observable} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {BreadcrumbService} from "../../../common/breadcrumb.service";

describe('LogbookDetailsComponent', () => {
  let component: LogbookDetailsComponent;
  let fixture: ComponentFixture<LogbookDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookDetailsComponent ],
      providers: [
        BreadcrumbService,
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 1})} }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
