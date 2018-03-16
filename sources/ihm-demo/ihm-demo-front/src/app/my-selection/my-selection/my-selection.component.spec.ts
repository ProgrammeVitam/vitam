import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';

import {MySelectionComponent} from './my-selection.component';
import {BreadcrumbService} from '../../common/breadcrumb.service';
import {ArchiveUnitHelper} from '../../archive-unit/archive-unit.helper';
import {MySelectionService} from '../../../../target/classes/my-selection/my-selection.service';
import {VitamResponse} from '../../../../target/classes/common/utils/response';
import {Observable} from 'rxjs/Rx';
import {RouterTestingModule} from '@angular/router/testing';

const MySelectionServiceStub = {
  getIdsToSelect: (isOperation, id) => Observable.of(new VitamResponse()),
  addToSelection: (withChildren: boolean, ids: string[], tenantId: string) => {}
};

describe('MySelectionComponent', () => {
  let component: MySelectionComponent;
  let fixture: ComponentFixture<MySelectionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule.withRoutes([
          {
            path: 'basket',
            component: MySelectionComponent
          },{
            path: 'ingest/sip', component: MySelectionComponent, data : {permission : 'ingest:create'}
          }
        ])
      ],
      declarations: [MySelectionComponent],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        //MySelectionService,
        { provide: MySelectionService, useValue: MySelectionServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MySelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
