import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ArchiveUnitFacetComponent } from './archive-unit-facet.component';
import { ResourcesService } from '../../common/resources.service';
import { CookieService } from 'angular2-cookie/core';
import { HttpClient, HttpHandler } from '@angular/common/http';
import {DialogService} from '../../common/dialog/dialog.service';
import { Observable } from 'rxjs/Rx';

import { RouterTestingModule } from '@angular/router/testing';

describe('ArchiveUnitFacetComponent', () => {
  let component: ArchiveUnitFacetComponent;
  let fixture: ComponentFixture<ArchiveUnitFacetComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      declarations: [ ArchiveUnitFacetComponent ],
      providers: [
        ResourcesService,
        CookieService,
        HttpClient,
        HttpHandler,
        DialogService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ArchiveUnitFacetComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitFacetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
