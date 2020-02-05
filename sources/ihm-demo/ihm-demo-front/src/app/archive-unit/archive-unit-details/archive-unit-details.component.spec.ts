import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ArchiveUnitDetailsComponent } from './archive-unit-details.component';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { VitamResponse } from "../../common/utils/response";
import { ArchiveUnitService } from "../archive-unit.service";
import { DialogService } from "../../common/dialog/dialog.service";
import { NavigationEnd } from "@angular/router";
import { ErrorService } from "../../common/error.service";

let DefaultResponse = {
  $context: {},
  $hits: {},
  $results: [{'#object': '', '#operations': ['operationId']}],
  httpCode: 200
};

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(DefaultResponse),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse()),
  getDetailsWithInheritedRules: (id: string) => Observable.of(DefaultResponse)
};

describe('ArchiveUnitDetailsComponent', () => {
  let component: ArchiveUnitDetailsComponent;
  let fixture: ComponentFixture<ArchiveUnitDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([
          {
            path: 'search/archiveUnit/:id',
            component: ArchiveUnitDetailsComponent
          },{
            path: 'ingest/sip', component: ArchiveUnitDetailsComponent, data : {permission : 'ingest:create'}
          }
        ])
      ],
      declarations: [ ArchiveUnitDetailsComponent ],
      providers: [
        BreadcrumbService,
        DialogService,
        ErrorService,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveUnitDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should reinit page when archiveUnitDetail is called', (done) => {
    spyOn(component, 'pageOnInit').and.callFake(() => {
      done();
    });
    spyOn(component.router, 'events').and.returnValues(
      Observable.of(new NavigationEnd(1, 'search/archiveUnit/myId', 'urlAfterRedirect'))
    );
    component.router.navigateByUrl('search/archiveUnit/myId');
  });

  it('should not reinit page when other page is called', (done) => {
    spyOn(component, 'pageOnInit').and.callFake(() => {
      fail('Should not call pageOnInit');
    });
    spyOn(component.router, 'events').and.returnValues(
      Observable.of(new NavigationEnd(1, 'ingest/sip', 'urlAfterRedirect'))
    );
    component.ngOnDestroy();
    component.router.navigateByUrl('ingest/sip');

    // If no failure after 2sec, we can consider that the test is OK !
    setTimeout(done, 2000);
  });

});
